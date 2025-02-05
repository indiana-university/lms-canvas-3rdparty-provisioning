package edu.iu.uits.lms.provisioning.service;

/*-
 * #%L
 * lms-lti-3rdpartyprovisioning
 * %%
 * Copyright (C) 2015 - 2022 Indiana University
 * %%
 * Redistribution and use in source and binary forms, with or without modification,
 * are permitted provided that the following conditions are met:
 * 
 * 1. Redistributions of source code must retain the above copyright notice, this
 *    list of conditions and the following disclaimer.
 * 
 * 2. Redistributions in binary form must reproduce the above copyright notice,
 *    this list of conditions and the following disclaimer in the documentation
 *    and/or other materials provided with the distribution.
 * 
 * 3. Neither the name of the Indiana University nor the names of its contributors
 *    may be used to endorse or promote products derived from this software without
 *    specific prior written permission.
 * 
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS" AND
 * ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE IMPLIED
 * WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE ARE DISCLAIMED.
 * IN NO EVENT SHALL THE COPYRIGHT HOLDER OR CONTRIBUTORS BE LIABLE FOR ANY DIRECT,
 * INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES (INCLUDING,
 * BUT NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES; LOSS OF USE,
 * DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND ON ANY THEORY OF
 * LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY, OR TORT (INCLUDING NEGLIGENCE
 * OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE OF THIS SOFTWARE, EVEN IF ADVISED
 * OF THE POSSIBILITY OF SUCH DAMAGE.
 * #L%
 */

import edu.iu.uits.lms.iuonly.model.acl.AuthorizedUser;
import edu.iu.uits.lms.iuonly.services.AuthorizedUserService;
import edu.iu.uits.lms.provisioning.Constants;
import edu.iu.uits.lms.provisioning.config.BackgroundMessage;
import edu.iu.uits.lms.provisioning.config.BackgroundMessageSender;
import edu.iu.uits.lms.provisioning.model.FileUploadResult;
import edu.iu.uits.lms.provisioning.model.NotificationForm;
import edu.iu.uits.lms.provisioning.model.content.ByteArrayFileContent;
import edu.iu.uits.lms.provisioning.model.content.FileContent;
import edu.iu.uits.lms.provisioning.service.exception.FileParsingException;
import edu.iu.uits.lms.provisioning.service.exception.ZipException;
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

import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Iterator;
import java.util.List;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

@Slf4j
@Service
public class DeptProvFileUploadService {

   @Autowired
   private DeptRouter deptRouter;

   @Autowired
   private BackgroundMessageSender backgroundMessageSender;

   @Autowired
   private AuthorizedUserService authorizedUserService;

   /**
    *
    * @param file Input zip file
    * @return File object representing the directory where all the files were unzipped
    * @throws IOException Throws IOException when there are file related issues
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
            files.add(new CommonMultipartFile(fileItem));
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
    * @param files List of files to parse
    * @param customUsersNotification Flag indicating if a custom user notification is desired
    * @param department Department the upload is for
    * @param principal Principal
    * @param source Source of the upload
    * @return ResponseEntity with the FileUploadResult
    */
   public ResponseEntity<FileUploadResult> parseFiles(MultipartFile[] files, boolean customUsersNotification,
                                                      String department, Object principal, Constants.SOURCE source) {
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

         backgroundMessageSender.send(new BackgroundMessage(filesByType, department, notificationForm, archiveId, validUsername, source));
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
    * @param principal Principal object
    * @return Validated Username
    * @throws UserAuthException When user is not authorized
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
         AuthorizedUser user = authorizedUserService.findByActiveUsernameAndToolPermission(username, Constants.AUTH_USER_TOOL_PERMISSION);
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

   /**
    * Custom implementation of a MultipartFile
    */
   public static class CommonMultipartFile implements MultipartFile {

      private FileItem fileItem;

      public CommonMultipartFile(FileItem fileItem) {
         this.fileItem = fileItem;
      }

      @Override
      public String getName() {
         return fileItem.getFieldName();
      }

      @Override
      public String getOriginalFilename() {
         return fileItem.getName();
      }

      @Override
      public String getContentType() {
         return fileItem.getContentType();
      }

      @Override
      public boolean isEmpty() {
         return getSize() == 0;
      }

      @Override
      public long getSize() {
         return fileItem.getSize();
      }

      @Override
      public byte[] getBytes() throws IOException {
         return fileItem.get();
      }

      @Override
      public InputStream getInputStream() throws IOException {
         return fileItem.getInputStream();
      }

      @Override
      public void transferTo(File dest) throws IOException, IllegalStateException {
//         fileItem.
              throw new UnsupportedOperationException("Not implemented yet");
      }
   }

}
