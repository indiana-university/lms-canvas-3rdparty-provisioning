package edu.iu.uits.lms.provisioning.service;

import canvas.client.generated.api.CanvasApi;
import canvas.client.generated.api.ImportApi;
import canvas.client.generated.model.CanvasUploadStatus;
import com.fasterxml.jackson.databind.ObjectMapper;
import edu.iu.uits.lms.provisioning.config.ToolConfig;
import edu.iu.uits.lms.provisioning.repository.CanvasImportIdRepository;
import email.client.generated.api.EmailApi;
import iuonly.client.generated.api.BatchEmailApi;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.io.IOUtils;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Bean;
import org.springframework.http.converter.json.Jackson2ObjectMapperBuilder;
import org.springframework.test.context.junit4.SpringRunner;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.HashMap;

import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.when;

@Slf4j
@RunWith(SpringRunner.class)
public class EmailSummaryServiceTest {

   @Autowired
   private EmailSummaryService emailSummaryService;

   @MockBean
   private CanvasImportIdRepository canvasImportIdRepository;

   @MockBean
   private EmailApi emailApi;

   @MockBean
   private BatchEmailApi batchEmailApi;

   @MockBean
   private ImportApi importApi;

   @MockBean
   private CanvasApi canvasApi;

   @MockBean
   private DeptRouter deptRouter;

   @MockBean
   private ToolConfig toolConfig;

   @TestConfiguration
   static class TestContextConfiguration {
      @Bean
      public EmailSummaryService emailSummaryService() {
         return new EmailSummaryService();
      }

   }

   @Before
   public void setUp() throws Exception {
      when(canvasApi.getBaseUrl()).thenReturn("foo.bar");
   }

   @Test
   public void testProcessImport() throws Exception {
      testProcessImport("1_import.json", "1_email.txt");
   }

   @Test
   public void testProcessImport2() throws Exception {
      testProcessImport("2_import.json", "2_email.txt");
   }

   @Test
   public void testProcessImport3() throws Exception {
      testProcessImport("3_import.json", "3_email.txt");
   }

   @Test
   public void testProcessImport4() throws Exception {
      testProcessImport("4_import.json", "4_email.txt");
   }


   private void testProcessImport(String jsonFile, String emailFile) throws Exception {
      CanvasUploadStatus canvasUploadStatus = loadData(jsonFile);
      when(importApi.getImportStatus(anyString())).thenReturn(canvasUploadStatus);
      EmailSummaryService.CanvasImportObject cio = new EmailSummaryService.CanvasImportObject();
      StringBuilder sb = new StringBuilder();

      emailSummaryService.processImport("7413704", sb, new HashMap<>(), cio, new ArrayList<>());
      log.debug("{}", sb);

      String expectedEmail = getEmailResult(emailFile);
      Assert.assertEquals("emails don't match", expectedEmail, sb.toString());
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
