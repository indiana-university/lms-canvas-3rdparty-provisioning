package edu.iu.uits.lms.provisioning;

import edu.iu.uits.lms.provisioning.config.BackgroundMessage;
import edu.iu.uits.lms.provisioning.config.BackgroundMessageListener;
import edu.iu.uits.lms.provisioning.controller.Constants;
import edu.iu.uits.lms.provisioning.repository.CanvasImportIdRepository;
import edu.iu.uits.lms.provisioning.service.DeptRouter;
import edu.iu.uits.lms.provisioning.service.exception.FileProcessingException;
import edu.iu.uits.lms.provisioning.service.exception.FileUploadException;
import edu.iu.uits.lms.provisioning.service.exception.ZipException;
import email.client.generated.api.EmailApi;
import iuonly.client.generated.api.BatchEmailApi;
import iuonly.client.generated.model.LmsBatchEmail;
import lombok.extern.slf4j.Slf4j;
import org.junit.Ignore;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Bean;
import org.springframework.test.context.junit4.SpringRunner;

import java.util.Collections;

import static org.mockito.ArgumentMatchers.any;

@Slf4j
@RunWith(SpringRunner.class)
@Ignore
public class BackgroundMessageListenerTest {

   @Autowired
   private BackgroundMessageListener backgroundMessageListener;

   @MockBean
   private DeptRouter deptRouter;

   @MockBean
   private CanvasImportIdRepository canvasImportIdRepository;

   @MockBean
   private EmailApi emailApi;

   @MockBean
   private BatchEmailApi batchEmailApi;

   @Test
   public void testListenWithFileProcessingException() throws Exception {
      BackgroundMessage bm = new BackgroundMessage(null, "cwm", null, null, "chmaurer", Constants.SOURCE.APP);

      FileProcessingException fpe = new FileProcessingException("uh oh", Collections.singletonList("file.txt"));
      Mockito.when(deptRouter.processFiles(any(), any(), any())).thenThrow(fpe);

      LmsBatchEmail lbe = new LmsBatchEmail();
      lbe.setGroupCode("cwm");
      lbe.setEmails("chmaurer@iu.edu");
      Mockito.when(batchEmailApi.getBatchEmailFromGroupCode(any())).thenReturn(lbe);

      backgroundMessageListener.handleMessage(bm);
   }

   @Test
   public void testListenWithFileUploadException() throws Exception {
      BackgroundMessage bm = new BackgroundMessage(null, "cwm", null, null, "chmaurer", Constants.SOURCE.APP);

      FileUploadException fue = new FileUploadException("uh oh");
      Mockito.when(deptRouter.sendToCanvas(any(), any(), any(), any(), any(), any())).thenThrow(fue);

      LmsBatchEmail lbe = new LmsBatchEmail();
      lbe.setGroupCode("cwm");
      lbe.setEmails("chmaurer@iu.edu");
      Mockito.when(batchEmailApi.getBatchEmailFromGroupCode(any())).thenReturn(lbe);

      backgroundMessageListener.handleMessage(bm);
   }

   @Test
   public void testListenWithZipException() throws Exception {
      BackgroundMessage bm = new BackgroundMessage(null, "cwm", null, null, "chmaurer", Constants.SOURCE.APP);

      ZipException ze = new ZipException("foo.zip", false, "uh oh");
      Mockito.when(deptRouter.sendToCanvas(any(), any(), any(), any(), any(), any())).thenThrow(ze);

      LmsBatchEmail lbe = new LmsBatchEmail();
      lbe.setGroupCode("cwm");
      lbe.setEmails("chmaurer@iu.edu");
      Mockito.when(batchEmailApi.getBatchEmailFromGroupCode(any())).thenReturn(lbe);

      backgroundMessageListener.handleMessage(bm);
   }

   @TestConfiguration
   static class BackgroundMessageListenerTestContextConfiguration {
      @Bean
      public BackgroundMessageListener backgroundMessageListener() {
         return new BackgroundMessageListener();
      }

   }
}
