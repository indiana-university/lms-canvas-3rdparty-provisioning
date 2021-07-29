package edu.iu.uits.lms.provisioning.service;

import edu.iu.uits.lms.provisioning.config.BackgroundMessage;
import edu.iu.uits.lms.provisioning.config.BackgroundMessageSender;
import edu.iu.uits.lms.provisioning.model.FileUploadResult;
import edu.iu.uits.lms.provisioning.model.NotificationForm;
import edu.iu.uits.lms.provisioning.model.content.ByteArrayFileContent;
import edu.iu.uits.lms.provisioning.model.content.FileContent;
import edu.iu.uits.lms.provisioning.service.exception.FileParsingException;
import edu.iu.uits.lms.provisioning.service.exception.ZipException;
import iuonly.client.generated.api.DeptProvisioningUserApi;
import iuonly.client.generated.model.DeptProvisioningUser;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.collections4.MultiValuedMap;
import org.apache.commons.fileupload.FileItem;
import org.apache.commons.fileupload.disk.DiskFileItem;
import org.apache.commons.io.IOUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.multipart.commons.CommonsMultipartFile;

import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Iterator;
import java.util.List;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

@Slf4j
@Service
public class FileUploadService {

   @Autowired
   private DeptRouter deptRouter;

   @Autowired
   private BackgroundMessageSender backgroundMessageSender;

   @Autowired
   private DeptProvisioningUserApi deptProvisioningUserApi;

   /**
    *
    * @param file Input zip file
    * @return File object representing the directory where all the files were unzipped
    * @throws IOException
    */
   public List<MultipartFile> unzip(MultipartFile file) throws IOException {
      List<MultipartFile> files = new ArrayList<>();
      byte[] buffer = new byte[1024];
      File destDir = Files.createTempDirectory("ZipUpload").toFile();
      ZipInputStream zis = new ZipInputStream(file.getInputStream());
      ZipEntry zipEntry = zis.getNextEntry();
      while (zipEntry != null) {
         File newFile = newFile(destDir, zipEntry);
         File parent = newFile.getParentFile();
         if (zipEntry.isDirectory()) {
            if (!newFile.isDirectory() && !newFile.mkdirs()) {
               throw new IOException("Failed to create directory " + newFile);
            }
         } else if (parent.getName().equals("__MACOSX")) {
            //skip it
            log.warn("Skipping over a file/folder we don't want to process: {}/{}", parent.getName(), newFile.getName());
         } else {
            // fix for Windows-created archives
            if (!parent.isDirectory() && !parent.mkdirs()) {
               throw new IOException("Failed to create directory " + parent);
            }

            // write file content
            FileOutputStream fos = new FileOutputStream(newFile);
            int len;
            while ((len = zis.read(buffer)) > 0) {
               fos.write(buffer, 0, len);
            }
            fos.close();

            //Turn into multipart
            FileItem fileItem = new DiskFileItem("mainFile", Files.probeContentType(newFile.toPath()),
                  false, newFile.getName(), (int) newFile.length(), newFile.getParentFile());

            IOUtils.copy(new FileInputStream(newFile), fileItem.getOutputStream());
            files.add(new CommonsMultipartFile(fileItem));
         }
         zipEntry = zis.getNextEntry();
      }
      zis.closeEntry();
      zis.close();
      return files;
   }

   private File newFile(File destinationDir, ZipEntry zipEntry) throws IOException {
      File destFile = new File(destinationDir, zipEntry.getName());

      String destDirPath = destinationDir.getCanonicalPath();
      String destFilePath = destFile.getCanonicalPath();

      if (!destFilePath.startsWith(destDirPath + File.separator)) {
         throw new IOException("Entry is outside of the target dir: " + zipEntry.getName());
      }

      return destFile;
   }

   /**
    *
    * @param files
    * @param customUsersNotification
    * @param department
    * @param principal
    * @return
    */
   public ResponseEntity<FileUploadResult> parseFiles(MultipartFile[] files, boolean customUsersNotification,
                                                      String department, Object principal) {
      if (files == null || files.length == 0) {
         return ResponseEntity.badRequest().body(new FileUploadResult("No files to process"));
      }
      try {
         String validUsername = getValidatedUsername(principal);
         MultiValuedMap<DeptRouter.CSV_TYPES, FileContent> filesByType = FileParsingUtil.parseFiles(files, customUsersNotification);
         Long archiveId = deptRouter.zipOriginals(filesByType.get(DeptRouter.CSV_TYPES.ORIGINALS), department, validUsername);
         NotificationForm notificationForm = parsePropertiesFile(filesByType.get(DeptRouter.CSV_TYPES.PROPERTIES));
         if (customUsersNotification && notificationForm == null) {
            FileUploadResult results = new FileUploadResult("User indicated that custom user notification was desired, but either no message.properties file was supplied, or it was malformed");
            return ResponseEntity.badRequest().body(results);
         }

         backgroundMessageSender.send(new BackgroundMessage(filesByType, department, notificationForm, archiveId, validUsername));
      } catch (FileParsingException | ZipException e) {
         log.error("error parsing uploaded files", e);
         FileUploadResult results = new FileUploadResult("Error parsing uploaded files", e.getFileErrors());
         return ResponseEntity.badRequest().body(results);
      } catch (UserAuthException e) {
         log.error("Unable to get username for authz", e);
         return ResponseEntity.status(e.getHttpStatus()).body(new FileUploadResult(e.getMessage()));
      }
      return ResponseEntity.ok(new FileUploadResult("The files are being processed. Summary emails will be sent at a later time."));
   }

   /**
    *
    * @param principal
    * @return
    * @throws UserAuthException
    */
   protected String getValidatedUsername(Object principal) throws UserAuthException {
      log.debug("Principal: {}", principal);
      if (principal instanceof Jwt) {
         Jwt jwt = ((Jwt) principal);
         String username = jwt.getClaimAsString("user_name");
         log.debug("Username from Jwt: {}", username);
         if (username == null) {
            //Fall back to the client id
            username = jwt.getClaimAsString("client_id");
            log.debug("client id from Jwt: {}", username);
         }
         DeptProvisioningUser user = deptProvisioningUserApi.findByUsername(username);
         log.debug("User: {}", user);
         if (user != null) {
            return user.getUsername();
         } else {
            throw new UserAuthException("User (" + username + ") is not authorized to upload files", username, HttpStatus.FORBIDDEN);
         }
      }
      throw new UserAuthException("No username could be found for authorization", null, HttpStatus.UNAUTHORIZED);
   }

   private NotificationForm parsePropertiesFile(Collection<FileContent> propFiles) {
      Iterator<FileContent> iterator = propFiles.iterator();
      if (iterator.hasNext()) {
         FileContent fc = iterator.next();

         byte[] fileBytes = ((ByteArrayFileContent)fc).getContents();
         CustomNotificationBuilder cnb = new CustomNotificationBuilder(new ByteArrayInputStream(fileBytes));
         if (cnb.isFileExists() && cnb.isFileValid()) {
            NotificationForm notificationForm = new NotificationForm();
            notificationForm.setBody(cnb.getBody());
            notificationForm.setSender(cnb.getSender());
            notificationForm.setSubject(cnb.getSubject());
            return notificationForm;
         }
      }
      return null;
   }

   @Getter
   public static class UserAuthException extends Exception {
      private String username;
      private HttpStatus httpStatus;

      public UserAuthException(String message, String username, HttpStatus httpStatus) {
         super(message);
         this.username = username;
         this.httpStatus = httpStatus;
      }
   }

}
