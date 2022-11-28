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

import edu.iu.uits.lms.iuonly.model.DeptProvisioningUser;
import edu.iu.uits.lms.iuonly.services.DeptProvisioningUserServiceImpl;
import edu.iu.uits.lms.lti.config.TestUtils;
import edu.iu.uits.lms.provisioning.config.BackgroundMessageSender;
import edu.iu.uits.lms.provisioning.controller.Constants;
import edu.iu.uits.lms.provisioning.model.FileUploadResult;
import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.io.InputStream;
import java.util.Comparator;
import java.util.List;

import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.when;

@Slf4j
@SpringBootTest(classes = DeptProvFileUploadService.class)
public class FileUploadServiceTest {

   private static final String OK_MESSAGE = "The files are being processed. Summary emails will be sent at a later time.";
   private static final String BAD_PROPS_MESSAGE = "User indicated that custom user notification was desired, but either no message.properties file was supplied, or it was malformed";

   @Autowired
   private DeptProvFileUploadService fileUploadService;

   @MockBean
   private DeptRouter deptRouter;

   @MockBean
   private BackgroundMessageSender backgroundMessageSender;

   @MockBean
   private DeptProvisioningUserServiceImpl deptProvisioningUserService;

   @Test
   public void testUnzip() throws Exception {
      MultipartFile file = mockFile("prov_files.zip");
      List<MultipartFile> files = fileUploadService.unzip(file);
      files.sort(Comparator.comparing(MultipartFile::getOriginalFilename));
      files.forEach(f -> log.debug(f.getOriginalFilename()));
      Assertions.assertEquals(2, files.size());
      Assertions.assertEquals("sections.csv", files.get(0).getOriginalFilename());
      Assertions.assertEquals("users.csv", files.get(1).getOriginalFilename());
   }

   @Test
   public void testGetValidatedUsernameWithNull() throws Exception {
      DeptProvFileUploadService.UserAuthException t = Assertions.assertThrows(DeptProvFileUploadService.UserAuthException.class, () -> fileUploadService.getValidatedUsername(null));
      Assertions.assertEquals("No username could be found for authorization", t.getMessage());
   }

   @Test
   public void testGetValidatedUsernameWithNullUsername() throws Exception {
      Jwt jwt = TestUtils.createJwtToken("asdf", null);
      DeptProvFileUploadService.UserAuthException t = Assertions.assertThrows(DeptProvFileUploadService.UserAuthException.class, () -> fileUploadService.getValidatedUsername(jwt));
      Assertions.assertEquals("User (asdf) is not authorized to upload files", t.getMessage());
   }

   @Test
   public void testGetValidatedUsernameWithClientAndUsername() throws Exception {
      Jwt jwt = TestUtils.createJwtToken("asdf", "foo");
      DeptProvFileUploadService.UserAuthException t = Assertions.assertThrows(DeptProvFileUploadService.UserAuthException.class, () -> fileUploadService.getValidatedUsername(jwt));
      Assertions.assertEquals("User (foo) is not authorized to upload files", t.getMessage());
   }

   @Test
   public void testGetValidatedUsernameWithBad() throws Exception {
      Jwt jwt = TestUtils.createJwtToken("asdf");
      DeptProvFileUploadService.UserAuthException t = Assertions.assertThrows(DeptProvFileUploadService.UserAuthException.class, () -> fileUploadService.getValidatedUsername(jwt));
      Assertions.assertEquals("User (asdf) is not authorized to upload files", t.getMessage());
   }

   @Test
   public void testGetValidatedUsernameWithGood() throws Exception {
      mockUser("asdf");
      Jwt jwt = TestUtils.createJwtToken("asdf");
      String username = fileUploadService.getValidatedUsername(jwt);
      Assertions.assertEquals("asdf", username);
   }

   @Test
   public void testParseNoFiles() throws Exception {
      mockUser("asdf");
      Jwt jwt = TestUtils.createJwtToken("asdf");
      ResponseEntity<FileUploadResult> responseEntity = fileUploadService.parseFiles(null, true, "CWM", jwt, Constants.SOURCE.API);
      Assertions.assertNotNull(responseEntity);
      Assertions.assertEquals(HttpStatus.BAD_REQUEST, responseEntity.getStatusCode());
      Assertions.assertEquals("No files to process", responseEntity.getBody().getMessage());
   }

   @Test
   public void testParseFilesNoProps() throws Exception {
      MultipartFile[] files = {mockFile("sections.csv"), mockFile("users.csv")};
      mockUser("asdf");
      Jwt jwt = TestUtils.createJwtToken("asdf");

      ResponseEntity<FileUploadResult> responseEntity = fileUploadService.parseFiles(files, false, "CWM", jwt, Constants.SOURCE.API);
      Assertions.assertNotNull(responseEntity);
      Assertions.assertEquals(HttpStatus.OK, responseEntity.getStatusCode());
      Assertions.assertEquals(OK_MESSAGE, responseEntity.getBody().getMessage());
   }

   @Test
   public void testParseFilesMissingProps() throws Exception {
      MultipartFile[] files = {mockFile("sections.csv"), mockFile("users.csv")};
      mockUser("asdf");
      Jwt jwt = TestUtils.createJwtToken("asdf");

      ResponseEntity<FileUploadResult> responseEntity = fileUploadService.parseFiles(files, true, "CWM", jwt, Constants.SOURCE.API);
      Assertions.assertNotNull(responseEntity);
      Assertions.assertEquals(HttpStatus.BAD_REQUEST, responseEntity.getStatusCode());
      Assertions.assertEquals(BAD_PROPS_MESSAGE, responseEntity.getBody().getMessage());
   }

   @Test
   public void testParseFilesWithBadProps() throws Exception {
      MultipartFile[] files = {mockFile("sections.csv"), mockFile("users.csv"), mockFile("bad_message.properties", "message.properties")};
      mockUser("asdf");
      Jwt jwt = TestUtils.createJwtToken("asdf");

      ResponseEntity<FileUploadResult> responseEntity = fileUploadService.parseFiles(files, true, "CWM", jwt, Constants.SOURCE.API);
      Assertions.assertNotNull(responseEntity);
      Assertions.assertEquals(HttpStatus.BAD_REQUEST, responseEntity.getStatusCode());
      Assertions.assertEquals(BAD_PROPS_MESSAGE, responseEntity.getBody().getMessage());
   }

   @Test
   public void testParseFilesWithBadPropsButDontCare() throws Exception {
      MultipartFile[] files = {mockFile("sections.csv"), mockFile("users.csv"), mockFile("bad_message.properties", "message.properties")};
      mockUser("asdf");
      Jwt jwt = TestUtils.createJwtToken("asdf");

      ResponseEntity<FileUploadResult> responseEntity = fileUploadService.parseFiles(files, false, "CWM", jwt, Constants.SOURCE.API);
      Assertions.assertNotNull(responseEntity);
      Assertions.assertEquals(HttpStatus.OK, responseEntity.getStatusCode());
      Assertions.assertEquals(OK_MESSAGE, responseEntity.getBody().getMessage());
   }

   @Test
   public void testParseFilesWithGoodProps() throws Exception {
      MultipartFile[] files = {mockFile("sections.csv"), mockFile("users.csv"), mockFile("good_message.properties", "message.properties")};
      mockUser("asdf");
      Jwt jwt = TestUtils.createJwtToken("asdf");

      ResponseEntity<FileUploadResult> responseEntity = fileUploadService.parseFiles(files, true, "CWM", jwt, Constants.SOURCE.API);
      Assertions.assertNotNull(responseEntity);
      Assertions.assertEquals(HttpStatus.OK, responseEntity.getStatusCode());
      Assertions.assertEquals(OK_MESSAGE, responseEntity.getBody().getMessage());
   }

   @Test
   public void testParseFilesWithGoodPropsButNoUsers() throws Exception {
      MultipartFile[] files = {mockFile("sections.csv"), mockFile("good_message.properties", "message.properties")};
      mockUser("asdf");
      Jwt jwt = TestUtils.createJwtToken("asdf");

      ResponseEntity<FileUploadResult> responseEntity = fileUploadService.parseFiles(files, true, "CWM", jwt, Constants.SOURCE.API);
      Assertions.assertNotNull(responseEntity);
      Assertions.assertEquals(HttpStatus.BAD_REQUEST, responseEntity.getStatusCode());
      Assertions.assertEquals("Error parsing uploaded files", responseEntity.getBody().getMessage());
      Assertions.assertEquals("No files found that match the 'users' format", responseEntity.getBody().getFileErrors().get(0).getTitle());
   }

   private void mockUser(String username) {
      DeptProvisioningUser user = new DeptProvisioningUser();
      user.setUsername(username);
      when(deptProvisioningUserService.findByUsername(anyString())).thenReturn(user);
   }

   private MultipartFile mockFile(String fileName) throws IOException {
      return mockFile(fileName, fileName);
   }

   private MultipartFile mockFile(String inputFileName, String resultFileName) throws IOException {
      InputStream fileStream = this.getClass().getResourceAsStream("/uploads/" + inputFileName);
      return new MockMultipartFile(resultFileName, resultFileName, "application/zip", fileStream);
   }
}
