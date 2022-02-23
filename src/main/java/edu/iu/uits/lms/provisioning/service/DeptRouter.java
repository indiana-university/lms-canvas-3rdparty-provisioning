package edu.iu.uits.lms.provisioning.service;

import canvas.client.generated.api.CanvasApi;
import canvas.client.generated.api.ImportApi;
import edu.iu.uits.lms.provisioning.controller.Constants;
import edu.iu.uits.lms.provisioning.model.CanvasImportId;
import edu.iu.uits.lms.provisioning.model.DeptProvArchive;
import edu.iu.uits.lms.provisioning.model.NotificationForm;
import edu.iu.uits.lms.provisioning.model.content.ByteArrayFileContent;
import edu.iu.uits.lms.provisioning.model.content.FileContent;
import edu.iu.uits.lms.provisioning.repository.ArchiveRepository;
import edu.iu.uits.lms.provisioning.repository.CanvasImportIdRepository;
import edu.iu.uits.lms.provisioning.service.exception.FileProcessingException;
import edu.iu.uits.lms.provisioning.service.exception.FileUploadException;
import edu.iu.uits.lms.provisioning.service.exception.ZipException;
import email.client.generated.api.EmailApi;
import email.client.generated.model.EmailDetails;
import iuonly.client.generated.api.BatchEmailApi;
import iuonly.client.generated.model.LmsBatchEmail;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.collections4.MultiValuedMap;
import org.apache.commons.io.IOUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

import static edu.iu.uits.lms.provisioning.service.ProvisioningResult.FileObject;


/**
 * Used to decide which classes get called
 */
@Slf4j
@Service
public class DeptRouter {

   public enum CSV_TYPES {
      COURSES,
      ENROLLMENTS,
      SECTIONS,
      USERS,
      EXPAND_ENROLLMENTS,
      BAD,
      ORIGINALS,
      PROPERTIES
   }

   @Autowired
   protected UserProvisioning userProvisioning;

   @Autowired
   private CourseProvisioning courseProvisioning;

   @Autowired
   private EnrollmentProvisioning enrollmentProvisioning;

   @Autowired
   private SectionProvisioning sectionProvisioning;

   @Autowired
   private ExpandEnrollmentProvisioning expandEnrollmentProvisioning;

   @Autowired
   private CanvasImportIdRepository canvasImportIdRepository;

   @Autowired
   private ImportApi importApi;

   @Autowired
   private EmailApi emailApi;

   @Autowired
   private CsvService csvService;

   @Autowired
   private BatchEmailApi batchEmailApi;

   @Autowired
   private CanvasApi canvasApi;

   @Autowired
   private ArchiveRepository archiveRepository;

   public List<ProvisioningResult> processFiles(String dept, MultiValuedMap<CSV_TYPES, FileContent> filesByType, NotificationForm notificationForm, boolean allowSis, List<String> authorizedAccounts, boolean overrideRestrictions) throws FileProcessingException {
      Collection<FileContent> userFiles = filesByType.get(CSV_TYPES.USERS);
      Collection<FileContent> courseFiles = filesByType.get(CSV_TYPES.COURSES);
      Collection<FileContent> enrollmentFiles = filesByType.get(CSV_TYPES.ENROLLMENTS);
      Collection<FileContent> sectionFiles = filesByType.get(CSV_TYPES.SECTIONS);
      Collection<FileContent> expandEnrollmentFiles = filesByType.get(CSV_TYPES.EXPAND_ENROLLMENTS);

      List<ProvisioningResult> allPrs = new ArrayList<>();

      //Users first
      allPrs.addAll(userProvisioning.processUsers(userFiles, new CustomNotificationBuilder(notificationForm), dept));

      allPrs.addAll(courseProvisioning.processCourses(courseFiles, authorizedAccounts, overrideRestrictions));
      allPrs.addAll(enrollmentProvisioning.processEnrollments(enrollmentFiles, allowSis, authorizedAccounts, overrideRestrictions));
      allPrs.addAll(sectionProvisioning.processSections(sectionFiles, authorizedAccounts, overrideRestrictions));

      //Defer processing of expand enrollments if there were user files provided
      allPrs.addAll(expandEnrollmentProvisioning.processEnrollments(expandEnrollmentFiles, !userFiles.isEmpty()));

      List<ProvisioningResult> fileErrors = allPrs.stream().filter(ProvisioningResult::isHasException).collect(Collectors.toList());
      if (!fileErrors.isEmpty()) {
         List<String> errors = new ArrayList<>();
         for (ProvisioningResult prError : fileErrors) {
            errors.add(prError.getFileObject().getFileName());
         }
         throw new FileProcessingException("Error processing some uploaded files", errors);
      }

      return allPrs;
   }

   public String sendToCanvas(List<FileObject> allStreams, String dept, StringBuilder emailMessage, Long archiveId,
                            String username, Constants.SOURCE source) throws FileUploadException, ZipException {
      String importId = null;
      String canvasUploadFileName = dept + "-upload.zip";
      boolean zipException = false;

      String guid = UUID.randomUUID().toString();
      String zipPath = null;
      try {
         zipPath = Files.createTempDirectory(guid).toString();
      } catch (IOException e) {
         log.error("Unable to create temp dir", e);
         throw new FileUploadException("Unable to create temp directory for csv storage");
      }

      String canvasUploadFileNameFullPath = zipPath + "/" + canvasUploadFileName;

      // Zip up csv files and send it to Canvas!
      if (!allStreams.isEmpty()) {
         File zipFile = csvService.zipCsv(allStreams, canvasUploadFileNameFullPath);
         importId = importApi.sendZipToCanvas(zipFile);

         if (archiveId != null) {
            DeptProvArchive archive = archiveRepository.findById(archiveId).orElse(null);
            if (archive == null) {
               archive = new DeptProvArchive();
               archive.setDepartment(dept);
               archive.setUsername(username);
            }
            archive.setCanvasDisplayName(canvasUploadFileName);
            try {
               byte[] bytes = IOUtils.toByteArray(new FileInputStream(zipFile));
               archive.setCanvasContent(bytes);
               archive.setCanvasImportId(importId);
               archiveRepository.save(archive);
            } catch (IOException e) {
               zipException = true;
            }
         }

         if (importId != null && !"".equals(importId)) {
            // store the Canvas importId
            CanvasImportId canvasImportId = new CanvasImportId(importId, "N", dept, source);
            canvasImportIdRepository.save(canvasImportId);
            zipFile.delete();
         }
      }

      if (emailMessage.length() > 0) {
         LmsBatchEmail emails = batchEmailApi.getBatchEmailFromGroupCode(dept);
         String[] emailAddresses = null;
         if (emails != null) {
            emailAddresses = emails.getEmails().split(",");
         }
         if (emailAddresses != null && emailAddresses.length > 0) {
            // create the subject line of the email
            String subject = emailApi.getStandardHeader() + " csv file upload(s) for " + dept;

            // piece standard info, the errorMessages, and resultsMessages together
            StringBuilder finalMessage = new StringBuilder();
            finalMessage.append("The preprocessing stage of your Canvas provisioning job is complete and the results are provided below. " +
                  "The final results from Canvas (" + canvasApi.getBaseUrl() + ") will be sent out at a later time and will inform you " +
                  "of any issues that may have been encountered while importing the data. Guest account provisioning is the exception and will have final results in this email.\r\n\r\n");
            if (emailMessage.length() > 0) {
               finalMessage.append("Results from the uploads: \r\n\r\n");
            }
            finalMessage.append(emailMessage);

            // email has been combined together, so send it!
            EmailDetails details = new EmailDetails();
            details.setRecipients(Arrays.asList(emailAddresses));
            details.setSubject(subject);
            details.setBody(finalMessage.toString());
            emailApi.sendEmail(details, true);

         } else {
            log.warn("No email addresses specified for the group code: '" + dept + "' so no status email will be sent.");
         }
      }
      if (zipException) {
         throw new ZipException(ZipException.CANVAS_ZIP, false, "Failed to archive canvas zip file");
      }
      return importId;
   }

   public Long zipOriginals(Collection<FileContent> files, String dept, String username) throws ZipException {
      try {
         String guid = UUID.randomUUID().toString();
         String zipPath = Files.createTempDirectory(guid).toString();

         String originalFileNameFullPath = zipPath + "/originalFiles.zip";

         List<FileObject> fileObjects = files.stream()
               .map((FileContent file) -> new FileObject(file.getFileName(), ((ByteArrayFileContent)file).getContents()))
               .collect(Collectors.toList());

         File originalsZip = csvService.zipCsv(fileObjects, originalFileNameFullPath);

         DeptProvArchive archive = new DeptProvArchive();
         archive.setDepartment(dept);
         archive.setOriginalDisplayName(originalsZip.getName());
         archive.setUsername(username);

         byte[] bytes = IOUtils.toByteArray(new FileInputStream(originalsZip));
         archive.setOriginalContent(bytes);
         archiveRepository.save(archive);
         originalsZip.delete();
         return archive.getId();
      } catch (IOException e) {
         throw new ZipException(ZipException.ORIGINALS_ZIP, true, "Error zipping up original uploaded files");
      }
   }
}