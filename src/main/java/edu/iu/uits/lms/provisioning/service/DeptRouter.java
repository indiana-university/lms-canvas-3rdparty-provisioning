package edu.iu.uits.lms.provisioning.service;

import canvas.client.generated.api.CanvasApi;
import canvas.client.generated.api.ImportApi;
import com.opencsv.CSVReader;
import com.opencsv.exceptions.CsvException;
import edu.iu.uits.lms.provisioning.model.CanvasImportId;
import edu.iu.uits.lms.provisioning.model.DeptProvArchive;
import edu.iu.uits.lms.provisioning.model.LmsBatchEmail;
import edu.iu.uits.lms.provisioning.model.NotificationForm;
import edu.iu.uits.lms.provisioning.model.content.FileContent;
import edu.iu.uits.lms.provisioning.model.content.InputStreamFileContent;
import edu.iu.uits.lms.provisioning.model.content.StringArrayFileContent;
import edu.iu.uits.lms.provisioning.repository.ArchiveRepository;
import edu.iu.uits.lms.provisioning.repository.CanvasImportIdRepository;
import edu.iu.uits.lms.provisioning.repository.LmsBatchEmailRepository;
import edu.iu.uits.lms.provisioning.service.exception.FileParsingException;
import edu.iu.uits.lms.provisioning.service.exception.FileProcessingException;
import edu.iu.uits.lms.provisioning.service.exception.FileUploadException;
import edu.iu.uits.lms.provisioning.service.exception.ZipException;
import email.client.generated.api.EmailApi;
import email.client.generated.model.EmailDetails;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.collections4.MultiValuedMap;
import org.apache.commons.collections4.multimap.ArrayListValuedHashMap;
import org.apache.commons.io.IOUtils;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
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
      ORIGINALS
   }

   private static final String PROP_FILE = "message.properties";

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
   private LmsBatchEmailRepository batchEmailRepository;

   @Autowired
   private CanvasApi canvasApi;

   @Autowired
   private ArchiveRepository archiveRepository;

   public List<ProvisioningResult> processFiles(String dept, MultiValuedMap<CSV_TYPES, FileContent> filesByType, NotificationForm notificationForm) throws FileProcessingException {
      Collection<FileContent> userFiles = filesByType.get(CSV_TYPES.USERS);
      Collection<FileContent> courseFiles = filesByType.get(CSV_TYPES.COURSES);
      Collection<FileContent> enrollmentFiles = filesByType.get(CSV_TYPES.ENROLLMENTS);
      Collection<FileContent> sectionFiles = filesByType.get(CSV_TYPES.SECTIONS);
      Collection<FileContent> expandEnrollmentFiles = filesByType.get(CSV_TYPES.EXPAND_ENROLLMENTS);

      List<ProvisioningResult> allPrs = new ArrayList<>();

      //Users first
      allPrs.add(userProvisioning.processUsers(userFiles, new CustomNotificationBuilder(notificationForm), dept));

      allPrs.addAll(courseProvisioning.processCourses(courseFiles));
      allPrs.addAll(enrollmentProvisioning.processEnrollments(enrollmentFiles));
      allPrs.addAll(sectionProvisioning.processSections(sectionFiles));
      allPrs.addAll(expandEnrollmentProvisioning.processEnrollments(expandEnrollmentFiles));

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

   public MultiValuedMap<CSV_TYPES, FileContent> parseFiles(MultipartFile[] files, boolean customUsersNotification) throws FileParsingException {
      MultiValuedMap<CSV_TYPES, FileContent> filesByType = new ArrayListValuedHashMap<>();
      List<String> errors = new ArrayList<>();
      boolean foundUsersFile = false;

      for (MultipartFile file : files) {
         try {
            InputStream inputStream = file.getInputStream();
            List<String[]> fileContents = new CSVReader(new InputStreamReader(inputStream)).readAll();
            FileContent fc = new StringArrayFileContent(file.getOriginalFilename(), fileContents);
            InputStreamFileContent isfc = new InputStreamFileContent(file.getOriginalFilename(), inputStream);
            filesByType.put(CSV_TYPES.ORIGINALS, isfc);

            //Git the first line, hopefully headers!
            String[] firstLine = fileContents.get(0);
            switch (StringUtils.join(firstLine, ",")) {
               case CsvService.COURSES_HEADER:
               case CsvService.COURSES_HEADER + CsvService.START_DATE:
               case CsvService.COURSES_HEADER + CsvService.END_DATE:
               case CsvService.COURSES_HEADER + CsvService.START_DATE + CsvService.END_DATE:
               case CsvService.COURSES_HEADER_NO_TERM:
               case CsvService.COURSES_HEADER_NO_TERM + CsvService.START_DATE:
               case CsvService.COURSES_HEADER_NO_TERM + CsvService.END_DATE:
               case CsvService.COURSES_HEADER_NO_TERM + CsvService.START_DATE + CsvService.END_DATE:
                  filesByType.put(CSV_TYPES.COURSES, fc);
                  break;
               case CsvService.ENROLLMENTS_HEADER:
               case CsvService.ENROLLMENTS_HEADER_SECTION_LIMIT:
                  filesByType.put(CSV_TYPES.ENROLLMENTS, fc);
                  break;
               case CsvService.SECTIONS_HEADER:
               case CsvService.SECTIONS_HEADER + CsvService.START_DATE:
               case CsvService.SECTIONS_HEADER + CsvService.END_DATE:
               case CsvService.SECTIONS_HEADER + CsvService.START_DATE + CsvService.END_DATE:
                  FileContent fcOverride = new InputStreamFileContent(file.getOriginalFilename(), file.getInputStream());
                  filesByType.put(CSV_TYPES.SECTIONS, fcOverride);
                  break;
               case CsvService.USERS_HEADER_NO_SHORT_NAME:
                  filesByType.put(CSV_TYPES.USERS, fc);
                  foundUsersFile = true;
                  break;
               case CsvService.EXPAND_ENROLLMENT_HEADER:
                  filesByType.put(CSV_TYPES.EXPAND_ENROLLMENTS, fc);
                  break;
               default:
                  filesByType.put(CSV_TYPES.BAD, fc);
                  errors.add(fc.getFileName());
                  break;
            }
         } catch (IOException | CsvException e) {
            log.error("Error processing " + file.getOriginalFilename(), e);
            errors.add(file.getOriginalFilename());
         }
      }
      boolean wantedButMissing = customUsersNotification && !foundUsersFile;
      if (!errors.isEmpty() || wantedButMissing) {
         throw new FileParsingException("Error parsing some uploaded files", errors, wantedButMissing);
      }
      return filesByType;
   }

   public void sendToCanvas(List<FileObject> allStreams, String dept, StringBuilder emailMessage, Long archiveId,
                            String username) throws FileUploadException, ZipException {
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
         String importId = importApi.sendZipToCanvas(zipFile);

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
            String archiveName = zipPath + importId + "_" + canvasUploadFileName;

            // store the Canvas importId and archive path
            CanvasImportId canvasImportId = new CanvasImportId(importId, "N", dept, archiveName);
            canvasImportIdRepository.save(canvasImportId);
            zipFile.delete();
         }
      }

      if (emailMessage.length() > 0) {
         LmsBatchEmail emails = batchEmailRepository.getBatchEmailFromGroupCode(dept);
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
   }

   public Long zipOriginals(Collection<FileContent> files, String dept, String username) throws ZipException {
      try {
         String guid = UUID.randomUUID().toString();
         String zipPath = Files.createTempDirectory(guid).toString();

         String originalFileNameFullPath = zipPath + "/originalFiles.zip";

         List<FileObject> fileObjects = files.stream()
               .map((FileContent file) -> new FileObject(file.getFileName(), ((InputStreamFileContent)file).getContents()))
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