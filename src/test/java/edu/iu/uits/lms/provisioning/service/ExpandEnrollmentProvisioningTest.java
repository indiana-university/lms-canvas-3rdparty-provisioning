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

import edu.iu.uits.lms.canvas.services.CatalogListingService;
import edu.iu.uits.lms.canvas.services.UserService;
import edu.iu.uits.lms.provisioning.model.content.FileContent;
import edu.iu.uits.lms.provisioning.model.content.StringArrayFileContent;
import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;

import java.util.ArrayList;
import java.util.List;

@Slf4j
@SpringBootTest(classes = ExpandEnrollmentProvisioning.class)
public class ExpandEnrollmentProvisioningTest {

   @Autowired
   private ExpandEnrollmentProvisioning expandEnrollmentProvisioning;

   @MockBean
   private UserService userService;

   @MockBean
   private CatalogListingService catalogListingService;

   @Test
   public void testDeferOneFile() {
      List<FileContent> files = new ArrayList<>();

      files.add(new StringArrayFileContent("file1.txt", createFile("asdf", "qwerty")));
      List<ProvisioningResult> results = expandEnrollmentProvisioning.processEnrollments(files, true);
      Assertions.assertNotNull(results);
      Assertions.assertEquals(1, results.size());

      String text1 = "file1.txt:\r\n\tProcessing of this file has been deferred until after all other files have been imported into Canvas\r\n";
      Assertions.assertEquals(text1, results.get(0).getEmailMessage().toString());
   }

   @Test
   public void testDeferTwoFiles() {
      List<FileContent> files = new ArrayList<>();

      files.add(new StringArrayFileContent("file1.txt", createFile("asdf", "qwerty")));
      files.add(new StringArrayFileContent("file2.txt", createFile("foo", "bar")));
      List<ProvisioningResult> results = expandEnrollmentProvisioning.processEnrollments(files, true);
      Assertions.assertNotNull(results);
      Assertions.assertEquals(2, results.size());

      String text1 = "file1.txt:\r\n\tProcessing of this file has been deferred until after all other files have been imported into Canvas\r\n";
      String text2 = "file2.txt:\r\n\tProcessing of this file has been deferred until after all other files have been imported into Canvas\r\n";
      Assertions.assertEquals(text1, results.get(0).getEmailMessage().toString());
      Assertions.assertEquals(text2, results.get(1).getEmailMessage().toString());
   }

   @Test
   public void testRunOneFile() {
      List<FileContent> files = new ArrayList<>();

      files.add(new StringArrayFileContent("file1.txt", createFile("asdf", "qwerty")));
      List<ProvisioningResult> results = expandEnrollmentProvisioning.processEnrollments(files, false);
      Assertions.assertNotNull(results);
      Assertions.assertEquals(1, results.size());

      String text1 = "file1.txt:\r\n" +
            "\tCould not find the canvas user identified by the csv supplied user id asdf\r\n" +
            "\tUsers failed to be added to expand's enrollment: 1\r\n" +
            "\tUsers successfully added to expand's enrollment: 0\r\n" +
            "\tTotal records processed: 1\r\n";
      Assertions.assertEquals(text1, results.get(0).getEmailMessage().toString());
   }

   @Test
   public void testRunTwoFiles() {
      List<FileContent> files = new ArrayList<>();

      files.add(new StringArrayFileContent("file1.txt", createFile("asdf", "qwerty")));
      files.add(new StringArrayFileContent("file2.txt", createFile("foo", "bar")));
      List<ProvisioningResult> results = expandEnrollmentProvisioning.processEnrollments(files, false);
      Assertions.assertNotNull(results);
      Assertions.assertEquals(2, results.size());

      String text1 = "file1.txt:\r\n" +
            "\tCould not find the canvas user identified by the csv supplied user id asdf\r\n" +
            "\tUsers failed to be added to expand's enrollment: 1\r\n" +
            "\tUsers successfully added to expand's enrollment: 0\r\n" +
            "\tTotal records processed: 1\r\n";
      Assertions.assertEquals(text1, results.get(0).getEmailMessage().toString());


      String text2 = "file2.txt:\r\n" +
            "\tCould not find the canvas user identified by the csv supplied user id foo\r\n" +
            "\tUsers failed to be added to expand's enrollment: 1\r\n" +
            "\tUsers successfully added to expand's enrollment: 0\r\n" +
            "\tTotal records processed: 1\r\n";
      Assertions.assertEquals(text2, results.get(1).getEmailMessage().toString());
   }

   private List<String[]> createFile(String col1, String col2) {
      List<String[]> list = new ArrayList<>(2);
      list.add(new String[]{"user_id,listing_id"});
      list.add(new String[]{col1, col2});
      return list;
   }
}
