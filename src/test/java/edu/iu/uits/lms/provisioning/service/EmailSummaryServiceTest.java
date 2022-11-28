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

import com.fasterxml.jackson.databind.ObjectMapper;
import edu.iu.uits.lms.canvas.model.uploadstatus.CanvasUploadStatus;
import edu.iu.uits.lms.canvas.services.CanvasService;
import edu.iu.uits.lms.canvas.services.ImportService;
import edu.iu.uits.lms.email.service.EmailService;
import edu.iu.uits.lms.iuonly.services.BatchEmailServiceImpl;
import edu.iu.uits.lms.provisioning.config.ToolConfig;
import edu.iu.uits.lms.provisioning.repository.CanvasImportIdRepository;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.io.IOUtils;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.converter.json.Jackson2ObjectMapperBuilder;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.HashMap;

import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.when;

@Slf4j
@SpringBootTest(classes = EmailSummaryService.class)
public class EmailSummaryServiceTest {

   @Autowired
   private EmailSummaryService emailSummaryService;

   @MockBean
   private CanvasImportIdRepository canvasImportIdRepository;

   @MockBean
   private EmailService emailService;

   @MockBean
   private BatchEmailServiceImpl batchEmailService;

   @MockBean
   private ImportService importService;

   @MockBean
   private CanvasService canvasService;

   @MockBean
   private DeptRouter deptRouter;

   @MockBean
   private ToolConfig toolConfig;

   @BeforeEach
   public void setUp() throws Exception {
      when(canvasService.getBaseUrl()).thenReturn("foo.bar");
   }

   @Test
   public void testProcessImportNoErrorsWarnings() throws Exception {
      // Test with no errors or warnings
      testProcessImport("good_import.json", "good_email.txt");
   }

   @Test
   public void testProcessImportWarnings() throws Exception {
      // Test with only warnings
      testProcessImport("warnings_import.json", "warnings_email.txt");
   }

   @Test
   public void testProcessImportErrors() throws Exception {
      // Test with only errors
      testProcessImport("errors_import.json", "errors_email.txt");
   }

   @Test
   public void testProcessImportWarningsAndErrors() throws Exception {
      // Test with both errors and warnings
      testProcessImport("bothbad_import.json", "bothbad_email.txt");
   }


   private void testProcessImport(String jsonFile, String emailFile) throws Exception {
      CanvasUploadStatus canvasUploadStatus = loadData(jsonFile);
      when(importService.getImportStatus(anyString())).thenReturn(canvasUploadStatus);
      EmailSummaryService.CanvasImportObject cio = new EmailSummaryService.CanvasImportObject();
      StringBuilder sb = new StringBuilder();

      emailSummaryService.processImport("7413704", sb, new HashMap<>(), cio, new ArrayList<>());
      log.debug("{}", sb);

      String expectedEmail = getEmailResult(emailFile);
      Assertions.assertEquals(expectedEmail, sb.toString(), "emails don't match");
   }

   private CanvasUploadStatus loadData(String fileName) throws IOException {
      InputStream fileStream = this.getClass().getResourceAsStream("/imports/" + fileName);
      String json = IOUtils.toString(fileStream, StandardCharsets.UTF_8);

      ObjectMapper objectMapper = Jackson2ObjectMapperBuilder.json().build();
      CanvasUploadStatus cus = objectMapper.readValue(json, CanvasUploadStatus.class);
      return cus;
   }

   private String getEmailResult(String fileName) throws IOException {
      InputStream fileStream = this.getClass().getResourceAsStream("/imports/" + fileName);
      String email = IOUtils.toString(fileStream, StandardCharsets.UTF_8);
      return email;
   }
}
