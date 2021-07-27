package edu.iu.uits.lms.provisioning.service;

import edu.iu.uits.lms.provisioning.TestUtils;
import edu.iu.uits.lms.provisioning.config.BackgroundMessageSender;
import edu.iu.uits.lms.provisioning.model.FileUploadResult;
import edu.iu.uits.lms.provisioning.model.User;
import edu.iu.uits.lms.provisioning.repository.UserRepository;
import lombok.extern.slf4j.Slf4j;
import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Bean;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.test.context.junit4.SpringRunner;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.io.InputStream;
import java.util.Comparator;
import java.util.List;

import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.when;

@Slf4j
@RunWith(SpringRunner.class)
public class FileUploadServiceTest {

   private static final String OK_MESSAGE = "The files are being processed. Summary emails will be sent at a later time.";
   private static final String BAD_PROPS_MESSAGE = "User indicated that custom user notification was desired, but either no message.properties file was supplied, or it was malformed";

   @Autowired
   private FileUploadService fileUploadService;

   @MockBean
   private DeptRouter deptRouter;

   @MockBean
   private BackgroundMessageSender backgroundMessageSender;

   @MockBean
   private UserRepository userRepository;

   @Test
   public void testUnzip() throws Exception {
      MultipartFile file = mockFile("prov_files.zip");
      List<MultipartFile> files = fileUploadService.unzip(file);
      files.sort(Comparator.comparing(MultipartFile::getOriginalFilename));
      files.forEach(f -> log.debug(f.getOriginalFilename()));
      Assert.assertEquals(2, files.size());
      Assert.assertEquals("sections.csv", files.get(0).getOriginalFilename());
      Assert.assertEquals("users.csv", files.get(1).getOriginalFilename());
   }

   @Test
   public void testGetValidatedUsernameWithNull() throws Exception {
      FileUploadService.UserAuthException t = Assert.assertThrows(FileUploadService.UserAuthException.class, () -> fileUploadService.getValidatedUsername(null));
      Assert.assertEquals("No username could be found for authorization", t.getMessage());
   }

   @Test
   public void testGetValidatedUsernameWithNullUsername() throws Exception {
      Jwt jwt = TestUtils.createJwtToken("asdf", null);
      FileUploadService.UserAuthException t = Assert.assertThrows(FileUploadService.UserAuthException.class, () -> fileUploadService.getValidatedUsername(jwt));
      Assert.assertEquals("User (asdf) is not authorized to upload files", t.getMessage());
   }

   @Test
   public void testGetValidatedUsernameWithClientAndUsername() throws Exception {
      Jwt jwt = TestUtils.createJwtToken("asdf", "foo");
      FileUploadService.UserAuthException t = Assert.assertThrows(FileUploadService.UserAuthException.class, () -> fileUploadService.getValidatedUsername(jwt));
      Assert.assertEquals("User (foo) is not authorized to upload files", t.getMessage());
   }

   @Test
   public void testGetValidatedUsernameWithBad() throws Exception {
      Jwt jwt = TestUtils.createJwtToken("asdf");
      FileUploadService.UserAuthException t = Assert.assertThrows(FileUploadService.UserAuthException.class, () -> fileUploadService.getValidatedUsername(jwt));
      Assert.assertEquals("User (asdf) is not authorized to upload files", t.getMessage());
   }

   @Test
   public void testGetValidatedUsernameWithGood() throws Exception {
      mockUser("asdf");
      Jwt jwt = TestUtils.createJwtToken("asdf");
      String username = fileUploadService.getValidatedUsername(jwt);
      Assert.assertEquals("asdf", username);
   }

   @Test
   public void testParseNoFiles() throws Exception {
      mockUser("asdf");
      Jwt jwt = TestUtils.createJwtToken("asdf");
      ResponseEntity<FileUploadResult> responseEntity = fileUploadService.parseFiles(null, true, "CWM", jwt);
      Assert.assertNotNull(responseEntity);
      Assert.assertEquals(HttpStatus.BAD_REQUEST, responseEntity.getStatusCode());
      Assert.assertEquals("No files to process", responseEntity.getBody().getMessage());
   }

   @Test
   public void testParseFilesNoProps() throws Exception {
      MultipartFile[] files = {mockFile("sections.csv"), mockFile("users.csv")};
      mockUser("asdf");
      Jwt jwt = TestUtils.createJwtToken("asdf");

      ResponseEntity<FileUploadResult> responseEntity = fileUploadService.parseFiles(files, false, "CWM", jwt);
      Assert.assertNotNull(responseEntity);
      Assert.assertEquals(HttpStatus.OK, responseEntity.getStatusCode());
      Assert.assertEquals(OK_MESSAGE, responseEntity.getBody().getMessage());
   }

   @Test
   public void testParseFilesMissingProps() throws Exception {
      MultipartFile[] files = {mockFile("sections.csv"), mockFile("users.csv")};
      mockUser("asdf");
      Jwt jwt = TestUtils.createJwtToken("asdf");

      ResponseEntity<FileUploadResult> responseEntity = fileUploadService.parseFiles(files, true, "CWM", jwt);
      Assert.assertNotNull(responseEntity);
      Assert.assertEquals(HttpStatus.BAD_REQUEST, responseEntity.getStatusCode());
      Assert.assertEquals(BAD_PROPS_MESSAGE, responseEntity.getBody().getMessage());
   }

   @Test
   public void testParseFilesWithBadProps() throws Exception {
      MultipartFile[] files = {mockFile("sections.csv"), mockFile("users.csv"), mockFile("bad_message.properties", "message.properties")};
      mockUser("asdf");
      Jwt jwt = TestUtils.createJwtToken("asdf");

      ResponseEntity<FileUploadResult> responseEntity = fileUploadService.parseFiles(files, true, "CWM", jwt);
      Assert.assertNotNull(responseEntity);
      Assert.assertEquals(HttpStatus.BAD_REQUEST, responseEntity.getStatusCode());
      Assert.assertEquals(BAD_PROPS_MESSAGE, responseEntity.getBody().getMessage());
   }

   @Test
   public void testParseFilesWithBadPropsButDontCare() throws Exception {
      MultipartFile[] files = {mockFile("sections.csv"), mockFile("users.csv"), mockFile("bad_message.properties", "message.properties")};
      mockUser("asdf");
      Jwt jwt = TestUtils.createJwtToken("asdf");

      ResponseEntity<FileUploadResult> responseEntity = fileUploadService.parseFiles(files, false, "CWM", jwt);
      Assert.assertNotNull(responseEntity);
      Assert.assertEquals(HttpStatus.OK, responseEntity.getStatusCode());
      Assert.assertEquals(OK_MESSAGE, responseEntity.getBody().getMessage());
   }

   @Test
   public void testParseFilesWithGoodProps() throws Exception {
      MultipartFile[] files = {mockFile("sections.csv"), mockFile("users.csv"), mockFile("good_message.properties", "message.properties")};
      mockUser("asdf");
      Jwt jwt = TestUtils.createJwtToken("asdf");

      ResponseEntity<FileUploadResult> responseEntity = fileUploadService.parseFiles(files, true, "CWM", jwt);
      Assert.assertNotNull(responseEntity);
      Assert.assertEquals(HttpStatus.OK, responseEntity.getStatusCode());
      Assert.assertEquals(OK_MESSAGE, responseEntity.getBody().getMessage());
   }

   @Test
   public void testParseFilesWithGoodPropsButNoUsers() throws Exception {
      MultipartFile[] files = {mockFile("sections.csv"), mockFile("good_message.properties", "message.properties")};
      mockUser("asdf");
      Jwt jwt = TestUtils.createJwtToken("asdf");

      ResponseEntity<FileUploadResult> responseEntity = fileUploadService.parseFiles(files, true, "CWM", jwt);
      Assert.assertNotNull(responseEntity);
      Assert.assertEquals(HttpStatus.BAD_REQUEST, responseEntity.getStatusCode());
      Assert.assertEquals("Error parsing uploaded files", responseEntity.getBody().getMessage());
      Assert.assertEquals("No files found that match the 'users' format", responseEntity.getBody().getFileErrors().get(0).getTitle());
   }

   private void mockUser(String username) {
      User user = new User();
      user.setUsername(username);
      when(userRepository.findByUsername(anyString())).thenReturn(user);
   }

   private MultipartFile mockFile(String fileName) throws IOException {
      return mockFile(fileName, fileName);
   }

   private MultipartFile mockFile(String inputFileName, String resultFileName) throws IOException {
      InputStream fileStream = this.getClass().getResourceAsStream("/uploads/" + inputFileName);
      return new MockMultipartFile(resultFileName, resultFileName, "application/zip", fileStream);
   }

   @TestConfiguration
   static class TestContextConfiguration {
      @Bean
      public FileUploadService fileUploadService() {
         return new FileUploadService();
      }

   }
}
