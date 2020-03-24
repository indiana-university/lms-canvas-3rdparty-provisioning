package edu.iu.uits.lms.provisioning.service;

import canvas.client.generated.api.CanvasApi;
import canvas.client.generated.api.ImportApi;
import com.opencsv.CSVReader;
import com.opencsv.exceptions.CsvException;
import edu.iu.uits.lms.provisioning.model.CanvasImportId;
import edu.iu.uits.lms.provisioning.model.LmsBatchEmail;
import edu.iu.uits.lms.provisioning.model.content.FileContent;
import edu.iu.uits.lms.provisioning.model.content.InputStreamFileContent;
import edu.iu.uits.lms.provisioning.model.content.StringArrayFileContent;
import edu.iu.uits.lms.provisioning.repository.CanvasImportIdRepository;
import edu.iu.uits.lms.provisioning.repository.LmsBatchEmailRepository;
import edu.iu.uits.lms.provisioning.service.exception.FileParsingException;
import edu.iu.uits.lms.provisioning.service.exception.FileProcessingException;
import edu.iu.uits.lms.provisioning.service.exception.FileUploadException;
import email.client.generated.api.EmailApi;
import email.client.generated.model.EmailDetails;
import lombok.extern.log4j.Log4j;
import org.apache.commons.collections4.MultiValuedMap;
import org.apache.commons.collections4.multimap.ArrayListValuedHashMap;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.file.Files;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Date;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;


/**
 * Used to decide which classes get called
 */
@Log4j
@Service
public class DeptRouter {

   public enum CSV_TYPES {
      COURSES,
      ENROLLMENTS,
      SECTIONS,
      USERS,
      EXPAND_ENROLLMENTS,
      PROPS,
      BAD
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

   public List<ProvisioningResult> processFiles(String dept, MultiValuedMap<CSV_TYPES, FileContent> filesByType, boolean customUsersNotification) throws FileProcessingException {
//      MultiValuedMap<CSV_TYPES, FileContent> filesByType = parseFiles(files);

      Collection<FileContent> userFiles = filesByType.get(CSV_TYPES.USERS);
      Collection<FileContent> courseFiles = filesByType.get(CSV_TYPES.COURSES);
      Collection<FileContent> enrollmentFiles = filesByType.get(CSV_TYPES.ENROLLMENTS);
      Collection<FileContent> sectionFiles = filesByType.get(CSV_TYPES.SECTIONS);
      Collection<FileContent> expandEnrollmentFiles = filesByType.get(CSV_TYPES.EXPAND_ENROLLMENTS);


      InputStreamFileContent propFile = (InputStreamFileContent) filesByType.get(CSV_TYPES.PROPS).stream().findFirst().orElse(new InputStreamFileContent(PROP_FILE, InputStream.nullInputStream()));

      StringBuilder emailMessage = new StringBuilder();

      //Users first
      emailMessage.append(userProvisioning.processUsers(userFiles, new CustomNotificationBuilder(propFile.getContents()), dept));

      List<ProvisioningResult> allPrs = new ArrayList<>();
      allPrs.addAll(courseProvisioning.processCourses(courseFiles));
      allPrs.addAll(enrollmentProvisioning.processEnrollments(enrollmentFiles));
      allPrs.addAll(sectionProvisioning.processSections(sectionFiles));
      allPrs.addAll(expandEnrollmentProvisioning.processEnrollments(expandEnrollmentFiles));

      List<ProvisioningResult> fileErrors = allPrs.stream().filter(pr -> pr.isHasException()).collect(Collectors.toList());
      if (!fileErrors.isEmpty()) {
         List<String> errors = new ArrayList<>();
         for (ProvisioningResult prError : fileErrors) {
            errors.add(prError.getFileObject().getFileName());
         }
         throw new FileProcessingException("Error processing some uploaded files", errors);
      }

      return allPrs;
   }

   public MultiValuedMap<CSV_TYPES, FileContent> parseFiles(MultipartFile[] files) throws FileParsingException {
      MultiValuedMap<CSV_TYPES, FileContent> filesByType = new ArrayListValuedHashMap<>();
      List<String> errors = new ArrayList<>();

      for (MultipartFile file : files) {
         try {
            if (PROP_FILE.equalsIgnoreCase(file.getOriginalFilename())) {
               FileContent fc = new InputStreamFileContent(file.getOriginalFilename(), file.getInputStream());
               filesByType.put(CSV_TYPES.PROPS, fc);
            } else {
               List<String[]> fileContents = new CSVReader(new InputStreamReader(file.getInputStream())).readAll();
               FileContent fc = new StringArrayFileContent(file.getOriginalFilename(), fileContents);

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
                     break;
                  case CsvService.EXPAND_ENROLLMENT_HEADER:
                     filesByType.put(CSV_TYPES.EXPAND_ENROLLMENTS, fc);
                     break;
                  default:
                     filesByType.put(CSV_TYPES.BAD, fc);
                     errors.add(fc.getFileName());
                     break;
               }
            }
         } catch (IOException | CsvException e) {
            log.error("Error processing " + file.getOriginalFilename(), e);
            errors.add(file.getOriginalFilename());
         }
      }
      if (!errors.isEmpty()) {
         throw new FileParsingException("Error parsing some uploaded files", errors);
      }
      return filesByType;
   }

   public void sendToCanvas(List<ProvisioningResult.FileObject> allStreams, String dept, StringBuilder emailMessage) throws FileUploadException {
//      String originalFileName = "originalFiles.zip";
//      String originalFileNameFullPath = zipPath + originalFileName;
      String canvasUploadFileName = dept + "-upload.zip";

      String guid = UUID.randomUUID().toString();
      String zipPath = null;
      try {
         zipPath = Files.createTempDirectory(guid).toString();
      } catch (IOException e) {
         log.error("Unable to create temp dir", e);
         throw new FileUploadException("Unable to create temp directory for csv storage");
      }

      String canvasUploadFileNameFullPath = zipPath + "/" + canvasUploadFileName;

      // Zip up the original file name
//      csvService.zipCsv(fileList, originalFileNameFullPath);

      // date stuff used for potential archive file naming
      Date date = new Date();
      SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd_HH-mm-ss");

      // Zip up csv files and send it to Canvas!
      if (!allStreams.isEmpty()) {
         File zipFile = csvService.zipCsv(allStreams, canvasUploadFileNameFullPath);
         String importId = importApi.sendZipToCanvas(zipFile);

         if (importId != null && !"".equals(importId)) {
            // archive the original and generated files
            List<File> combineZipsList = new ArrayList<>();
            combineZipsList.add(new File(canvasUploadFileNameFullPath));
//            combineZipsList.add(new File(originalFileNameFullPath));
            String archiveName = zipPath + importId + "_" + canvasUploadFileName;
//            csvService.zipCsv(combineZipsList, archiveName);

            // store the Canvas importId and archive path
            CanvasImportId canvasImportId = new CanvasImportId(importId, "N", dept, archiveName);
            canvasImportIdRepository.save(canvasImportId);
            zipFile.delete();
         } else {
            // This scenario is rare. File that should have been sent to Canvas but failed for whatever reason.
            // archive both files using a timestamp
            List<File> combineZipsList = new ArrayList<>();
            combineZipsList.add(new File(canvasUploadFileNameFullPath));
//            combineZipsList.add(new File(originalFileNameFullPath));

            String archiveName = zipPath + dateFormat.format(date) + "_" + dept + "_failedAttempt.zip";
//            csvService.zipCsv(combineZipsList, archiveName);
         }
//      } else {
//         // no files were attempted to send to Canvas, so do an alternative style of archiving
//         File oldFile = new File(originalFileNameFullPath);
//         File newFile = new File(zipPath + dateFormat.format(date) + "_" + dept + "_" + originalFileName);
//         oldFile.renameTo(newFile);
      }

      // clean up the files zipped and sent to Canvas to prevent a long-term storage issue
//      for (File file : filesToZip) {
//         file.delete();
//      }

//      // clean up the extraneous zip files
//      File originalFile = new File(originalFileNameFullPath);
//      originalFile.delete();
//      File canvasUploadFile = new File(canvasUploadFileNameFullPath);
//      canvasUploadFile.delete();

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
            emailApi.sendEmail(details);

         } else {
            log.warn("No email addresses specified for the group code: '" + dept + "' so no status email will be sent.");
         }
      }
//    }
   }

//    public static void main(String[] args) throws IOException {
//        // You need to pass in 1 argument for this code to run correctly!
//        // Argument 0 is the department code, e.g. GREGTEST1
//
//        ServiceLocator sl = new ServiceLocator();
//
//        propertiesService = sl.getService(PropertiesService.class);
//        emailService = sl.getService(EmailService.class);
//        dbConnectionService = sl.getService("dbConnectionService", DbConnectionService.class);
//        batchEmailLookupService = sl.getService(BatchEmailLookupService.class);
//        userService = sl.getService(UserService.class);
//        importService = sl.getService(ImportService.class);
//        csvService = sl.getService(CsvService.class);
//        amsService = sl.getService(AmsService.class);
//        passwordGenerator = sl.getService(PasswordGenerator.class);
//        canvasImportIdRepository = sl.getService(CanvasImportIdRepository.class);
//        jmsService = sl.getService(JmsService.class);
//        sisImportService = sl.getService(SisImportService.class);
//        customNotificationService = sl.getService(CustomNotificationService.class);
//
//        String canvasServer = propertiesService.getCanvasBaseUrl();
//        String filePath = "";
//        // Read in the arguments and assure it is in upper case
//        String dept = args[0].toUpperCase();
//        ENV_DIRECTORY = propertiesService.getAppEnv();
//        StringBuilder errorsMessage = new StringBuilder();
//        StringBuilder emailMessage = new StringBuilder();
//        String zipPath = filePath;
//
//        if ("".equals(dept)) {
//            log.info("An empty parameter was passed in for the department code. Exiting gracefully.");
//            return;
//        }
//
//        if ("".equals(ENV_DIRECTORY)) {
//            log.info("The ENV_DIRECTORY was empty. Exiting gracefully.");
//            return;
//        } else if (!"prd".equals(ENV_DIRECTORY) && !"stg".equals(ENV_DIRECTORY) && !"snd".equals(ENV_DIRECTORY) && !"reg".equals(ENV_DIRECTORY) && !"dev".equals(ENV_DIRECTORY)) {
//            log.info("The directory parameter passed in was not prd, stg, reg, or dev. Exiting gracefully.");
//            return;
//        }
//
//        // Read the csv file
//        try {
//            String hostname = InetAddress.getLocalHost().getHostName();
//            if (hostname.equals(PRODUCTION_HOST)) {
//                filePath = PRODUCTION_PATH + ENV_DIRECTORY + DEPT_DIR + dept + FILES_TO_PROCESS_PATH;
//                zipPath = PRODUCTION_PATH + ENV_DIRECTORY + DEPT_DIR + dept + "/";
//            } else if (hostname.equals(TEST_HOST)) {
//                filePath = TEST_PATH + ENV_DIRECTORY + DEPT_DIR + dept + FILES_TO_PROCESS_PATH;
//                zipPath = TEST_PATH + ENV_DIRECTORY + DEPT_DIR + dept + "/";
//            } else {
//                filePath = LOCAL_PATH + dept + FILES_TO_PROCESS_PATH;
//                zipPath = LOCAL_PATH + dept + "/";
//            }
//        } catch (UnknownHostException e) {
//            log.error(e);
//        }
//
//        // Code for reading csv files.  It is assumed the brte script will have copied the files from DTT/DTP
//        File folder = new File(filePath);
//
//        // make sure the directory exists before proceeding
//        if (!folder.exists()) {
//            // go ahead and make the directory
//            folder.mkdirs();
//        }
//
//        // cleanup older files from the archive
//        sisImportService.cleanDirectory(new File(zipPath), propertiesService.getDaysToKeepSisImportArchiveFiles());
//
//        // we only care about .csv files!
//        log.info("Filtering the files and only getting csv files from " + filePath);
//        List<File> fileList = csvService.filterFiles(folder);
//
//        if (fileList.isEmpty()) {
//            // if there is nothing to be read, go ahead and exit the code, since there's no point in uploading an empty zip
//            log.info("Exiting the code since there were no csv files found in " + filePath);
//            return;
//        }
//
//        // Assure the file list is in alphabetical order to match how Canvas will process them
//        Collections.sort(fileList);
//
//        List<File> filesToZip = new ArrayList<File>();
//
//        // look for user files first so new IU guest accounts are created before other file lookups happen
//        for (File filename : fileList) {
//            List <String[]> fileContents = new CSVReader(new FileReader(filename)).readAll();
//            for (String[] lineContentArray : fileContents) {
//                if (emailMessage.length() > 0) {
//                    emailMessage.append("\r\n");
//                }
//
//                String[] usersHeader = CsvService.USERS_HEADER_NO_SHORT_NAME.split(",");
//                if (Arrays.equals(lineContentArray,usersHeader)) {
//                    // route to UserProvisioning
//                    emailMessage.append(UserProvisioning.processUsers(filename, new CustomNotificationBuilder(filePath + "message.properties"), dept));
//                }
//                break;
//            }
//        }
//
//        // process the rest of the files!
//        for (File filename : fileList) {
//            List <String[]> fileContents = new CSVReader(new FileReader(filename)).readAll();
//            for (String[] lineContentArray : fileContents) {
//                ProvisioningResult pr = new ProvisioningResult();
//                if (emailMessage.length() > 0) {
//                    emailMessage.append("\r\n");
//                }
//
//                switch (StringUtils.join(lineContentArray,",")) {
//                    case CsvService.COURSES_HEADER:
//                    case CsvService.COURSES_HEADER + CsvService.START_DATE:
//                    case CsvService.COURSES_HEADER + CsvService.END_DATE:
//                    case CsvService.COURSES_HEADER + CsvService.START_DATE + CsvService.END_DATE:
//                    case CsvService.COURSES_HEADER_NO_TERM:
//                    case CsvService.COURSES_HEADER_NO_TERM + CsvService.START_DATE:
//                    case CsvService.COURSES_HEADER_NO_TERM + CsvService.END_DATE:
//                    case CsvService.COURSES_HEADER_NO_TERM + CsvService.START_DATE + CsvService.END_DATE:
//                        // route to CourseProvisioning
//                        pr = CourseProvisioning.processCourses(filename, dept);
//                        emailMessage.append(pr.getEmailMessage());
//                        filesToZip.add(pr.getFile());
//                        break;
//                    case CsvService.ENROLLMENTS_HEADER:
//                    case CsvService.ENROLLMENTS_HEADER_SECTION_LIMIT:
//                        // route to EnrollmentProvisioning
//                        pr = EnrollmentProvisioning.processEnrollments(filename, dept);
//                        emailMessage.append(pr.getEmailMessage());
//                        filesToZip.add(pr.getFile());
//                        break;
//                    case CsvService.SECTIONS_HEADER:
//                    case CsvService.SECTIONS_HEADER + CsvService.START_DATE:
//                    case CsvService.SECTIONS_HEADER + CsvService.END_DATE:
//                    case CsvService.SECTIONS_HEADER + CsvService.START_DATE + CsvService.END_DATE:
//                        // no need for a SectionProvisioning since we are not massaging any of the data
//                        emailMessage.append(filename.getName() + ":\r\n");
//                        emailMessage.append("\tSent to Canvas.\r\n");
//                        filesToZip.add(filename);
//                        break;
//                    case CsvService.USERS_HEADER_NO_SHORT_NAME:
//                        // we've already taken care of the user files, so just skip it
//                        break;
//                    case CsvService.EXPAND_ENROLLMENT_HEADER:
//                        // route to ExpandEnrollmentProvisioning
//                        pr = ExpandEnrollmentProvisoning.processEnrollments(filename, dept);
//                        break;
//                    default:
//                        // not a valid file, so prepare an email to send to addresses via what's in the database
//                        if (errorsMessage.length() == 0) {
//                            errorsMessage.append("The following csv files were not processed because the header was not correct or did not exist:\r\n\r\n");
//                        }
//
//                        errorsMessage.append(filename.getName() + "\r\n");
//                        break;
//                }
//                // need this additional break since the breaks in the switch statement seem to act like another loop
//                break;
//            }
//        }
//
//        String originalFileName = "originalFiles.zip";
//        String originalFileNameFullPath = zipPath + originalFileName;
//        String canvasUploadFileName = dept + "-upload.zip";
//        String canvasUploadFileNameFullPath = zipPath + canvasUploadFileName;
//
//        // Zip up the original file name
//        csvService.zipCsv(fileList, originalFileNameFullPath);
//
//        // date stuff used for potential archive file naming
//        Date date = new Date();
//        SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd_HH-mm-ss");
//
//        // Zip up csv files and send it to Canvas!
//        if (!filesToZip.isEmpty()) {
//            csvService.zipCsv(filesToZip, canvasUploadFileNameFullPath);
//            String importId = importService.sendZipToCanvas(canvasUploadFileNameFullPath);
//
//            if (importId != null && !"".equals(importId)) {
//                // archive the original and generated files
//                List<File> combineZipsList = new ArrayList<>();
//                combineZipsList.add(new File(canvasUploadFileNameFullPath));
//                combineZipsList.add(new File(originalFileNameFullPath));
//                String archiveName = zipPath + importId + "_" + canvasUploadFileName;
//                csvService.zipCsv(combineZipsList, archiveName);
//
//                // store the Canvas importId and archive path
//                CanvasImportId canvasImportId = new CanvasImportId(importId, "N", dept, archiveName);
//                canvasImportIdRepository.save(canvasImportId);
//            } else {
//                // This scenario is rare. File that should have been sent to Canvas but failed for whatever reason.
//                // archive both files using a timestamp
//                List<File> combineZipsList = new ArrayList<>();
//                combineZipsList.add(new File(canvasUploadFileNameFullPath));
//                combineZipsList.add(new File(originalFileNameFullPath));
//
//                String archiveName = zipPath + dateFormat.format(date) + "_" + dept + "_failedAttempt.zip";
//                csvService.zipCsv(combineZipsList, archiveName);
//            }
//        } else {
//            // no files were attempted to send to Canvas, so do an alternative style of archiving
//            File oldFile = new File(originalFileNameFullPath);
//            File newFile = new File(zipPath + dateFormat.format(date) + "_" + dept + "_" + originalFileName);
//            oldFile.renameTo(newFile);
//        }
//
//        // clean up the files zipped and sent to Canvas to prevent a long-term storage issue
//        for (File file : filesToZip) {
//            file.delete();
//        }
//
//        // clean up the extraneous zip files
//        File originalFile = new File(originalFileNameFullPath);
//        originalFile.delete();
//        File canvasUploadFile = new File(canvasUploadFileNameFullPath);
//        canvasUploadFile.delete();
//
//        if (emailMessage.length() > 0 || errorsMessage.length() > 0) {
//            String[] emailAddresses = batchEmailLookupService.lookupEmailsByGroupCode(dept);
//            if (emailAddresses != null && emailAddresses.length > 0) {
//                // create the subject line of the email
//                String subject = emailService.getStandardHeader() + " csv file upload(s) for " + dept;
//
//                // piece standard info, the errorMessages, and resultsMessages together
//                StringBuilder finalMessage = new StringBuilder();
//                finalMessage.append("The preprocessing stage of your Canvas provisioning job is complete and the results are provided below. The final results from Canvas (" + canvasServer + ") will be sent out at a later time and will inform you of any issues that may have been encountered while importing the data. Guest account provisioning is the exception and will have final results in this email.\r\n\r\n");
//                if (emailMessage.length() > 0) {
//                    finalMessage.append("Results from the uploads: \r\n\r\n");
//                }
//                finalMessage.append(emailMessage);
//                if (errorsMessage.length() > 0) {
//                    finalMessage.append("\r\n");
//                }
//                finalMessage.append(errorsMessage);
//
//                // email has been combined together, so send it!
//                try {
//                    emailService.sendEmail(emailAddresses, subject, finalMessage.toString());
//                } catch (LmsEmailTooBigException e) {
//                    // since this isn't using an attachment this exception should never be thrown
//                }
//            } else {
//                log.warn("No email addresses specified for the group code: '" + dept + "' so no status email will be sent.");
//            }
//        }
//    }
}