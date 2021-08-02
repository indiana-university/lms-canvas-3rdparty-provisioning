package edu.iu.uits.lms.provisioning.service;

import canvas.client.generated.api.ExpandApi;
import canvas.client.generated.api.UsersApi;
import edu.iu.uits.lms.provisioning.model.content.FileContent;
import edu.iu.uits.lms.provisioning.model.content.StringArrayFileContent;
import lombok.extern.slf4j.Slf4j;
import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Bean;
import org.springframework.test.context.junit4.SpringRunner;

import java.util.ArrayList;
import java.util.List;

@Slf4j
@RunWith(SpringRunner.class)
public class ExpandEnrollmentProvisioningTest {

   @Autowired
   private ExpandEnrollmentProvisioning expandEnrollmentProvisioning;

   @MockBean
   private UsersApi usersApi;

   @MockBean
   private ExpandApi expandApi;

   @Test
   public void testDeferOneFile() {
      List<FileContent> files = new ArrayList<>();

      files.add(new StringArrayFileContent("file1.txt", createFile("asdf", "qwerty")));
      List<ProvisioningResult> results = expandEnrollmentProvisioning.processEnrollments(files, true);
      Assert.assertNotNull(results);
      Assert.assertEquals(1, results.size());

      String text1 = "file1.txt:\r\n\tProcessing of this file has been deferred until after all other files have been imported into Canvas\r\n";
      Assert.assertEquals(text1, results.get(0).getEmailMessage().toString());
   }

   @Test
   public void testDeferTwoFiles() {
      List<FileContent> files = new ArrayList<>();

      files.add(new StringArrayFileContent("file1.txt", createFile("asdf", "qwerty")));
      files.add(new StringArrayFileContent("file2.txt", createFile("foo", "bar")));
      List<ProvisioningResult> results = expandEnrollmentProvisioning.processEnrollments(files, true);
      Assert.assertNotNull(results);
      Assert.assertEquals(2, results.size());

      String text1 = "file1.txt:\r\n\tProcessing of this file has been deferred until after all other files have been imported into Canvas\r\n";
      String text2 = "file2.txt:\r\n\tProcessing of this file has been deferred until after all other files have been imported into Canvas\r\n";
      Assert.assertEquals(text1, results.get(0).getEmailMessage().toString());
      Assert.assertEquals(text2, results.get(1).getEmailMessage().toString());
   }

   @Test
   public void testRunOneFile() {
      List<FileContent> files = new ArrayList<>();

      files.add(new StringArrayFileContent("file1.txt", createFile("asdf", "qwerty")));
      List<ProvisioningResult> results = expandEnrollmentProvisioning.processEnrollments(files, false);
      Assert.assertNotNull(results);
      Assert.assertEquals(1, results.size());

      String text1 = "file1.txt:\r\n" +
            "\tCould not find the canvas user identified by the csv supplied user id asdf\r\n" +
            "\tUsers failed to be added to expand's enrollment: 1\r\n" +
            "\tUsers successfully added to expand's enrollment: 0\r\n" +
            "\tTotal records processed: 1\r\n";
      Assert.assertEquals(text1, results.get(0).getEmailMessage().toString());
   }

   @Test
   public void testRunTwoFiles() {
      List<FileContent> files = new ArrayList<>();

      files.add(new StringArrayFileContent("file1.txt", createFile("asdf", "qwerty")));
      files.add(new StringArrayFileContent("file2.txt", createFile("foo", "bar")));
      List<ProvisioningResult> results = expandEnrollmentProvisioning.processEnrollments(files, false);
      Assert.assertNotNull(results);
      Assert.assertEquals(2, results.size());

      String text1 = "file1.txt:\r\n" +
            "\tCould not find the canvas user identified by the csv supplied user id asdf\r\n" +
            "\tUsers failed to be added to expand's enrollment: 1\r\n" +
            "\tUsers successfully added to expand's enrollment: 0\r\n" +
            "\tTotal records processed: 1\r\n";
      Assert.assertEquals(text1, results.get(0).getEmailMessage().toString());


      String text2 = "file2.txt:\r\n" +
            "\tCould not find the canvas user identified by the csv supplied user id foo\r\n" +
            "\tUsers failed to be added to expand's enrollment: 1\r\n" +
            "\tUsers successfully added to expand's enrollment: 0\r\n" +
            "\tTotal records processed: 1\r\n";
      Assert.assertEquals(text2, results.get(1).getEmailMessage().toString());
   }

   private List<String[]> createFile(String col1, String col2) {
      List<String[]> list = new ArrayList<>(2);
      list.add(new String[]{"user_id,listing_id"});
      list.add(new String[]{col1, col2});
      return list;
   }

   @TestConfiguration
   static class TestContextConfiguration {
      @Bean
      public ExpandEnrollmentProvisioning expandEnrollmentProvisioning() {
         return new ExpandEnrollmentProvisioning();
      }

   }
}
