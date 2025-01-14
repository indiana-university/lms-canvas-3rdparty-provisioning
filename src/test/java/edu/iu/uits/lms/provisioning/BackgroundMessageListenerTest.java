package edu.iu.uits.lms.provisioning;

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

import edu.iu.uits.lms.email.service.EmailService;
import edu.iu.uits.lms.iuonly.model.LmsBatchEmail;
import edu.iu.uits.lms.iuonly.services.BatchEmailServiceImpl;
import edu.iu.uits.lms.provisioning.config.BackgroundMessage;
import edu.iu.uits.lms.provisioning.config.BackgroundMessageListener;
import edu.iu.uits.lms.provisioning.repository.CanvasImportIdRepository;
import edu.iu.uits.lms.provisioning.service.DeptRouter;
import edu.iu.uits.lms.provisioning.service.exception.FileProcessingException;
import edu.iu.uits.lms.provisioning.service.exception.FileUploadException;
import edu.iu.uits.lms.provisioning.service.exception.ZipException;
import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Bean;

import java.util.Collections;

import static org.mockito.ArgumentMatchers.any;

@Slf4j
@Disabled
public class BackgroundMessageListenerTest {

   @Autowired
   private BackgroundMessageListener backgroundMessageListener;

   @MockBean
   private DeptRouter deptRouter;

   @MockBean
   private CanvasImportIdRepository canvasImportIdRepository;

   @MockBean
   private EmailService emailService;

   @MockBean
   private BatchEmailServiceImpl batchEmailService;

   @Test
   public void testListenWithFileProcessingException() throws Exception {
      BackgroundMessage bm = new BackgroundMessage(null, "cwm", null, null, "chmaurer", Constants.SOURCE.APP);

      FileProcessingException fpe = new FileProcessingException("uh oh", Collections.singletonList("file.txt"));
      Mockito.when(deptRouter.processFiles(any(), any(), any(), true, null, true)).thenThrow(fpe);

      LmsBatchEmail lbe = new LmsBatchEmail();
      lbe.setGroupCode("cwm");
      lbe.setEmails("chmaurer@iu.edu");
      Mockito.when(batchEmailService.getBatchEmailFromGroupCode(any())).thenReturn(lbe);

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
      Mockito.when(batchEmailService.getBatchEmailFromGroupCode(any())).thenReturn(lbe);

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
      Mockito.when(batchEmailService.getBatchEmailFromGroupCode(any())).thenReturn(lbe);

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
