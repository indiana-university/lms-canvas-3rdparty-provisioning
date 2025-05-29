package edu.iu.uits.lms.provisioning.service;

/*-
 * #%L
 * lms-lti-3rdpartyprovisioning
 * %%
 * Copyright (C) 2015 - 2024 Indiana University
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

import edu.iu.uits.lms.canvas.services.CanvasService;
import edu.iu.uits.lms.canvas.services.ImportService;
import edu.iu.uits.lms.email.model.EmailDetails;
import edu.iu.uits.lms.email.service.EmailService;
import edu.iu.uits.lms.iuonly.model.LmsBatchEmail;
import edu.iu.uits.lms.iuonly.services.BatchEmailServiceImpl;
import edu.iu.uits.lms.provisioning.Constants;
import edu.iu.uits.lms.provisioning.repository.ArchiveRepository;
import edu.iu.uits.lms.provisioning.repository.CanvasImportIdRepository;
import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.bean.override.mockito.MockitoBean;

import java.util.ArrayList;

import static org.mockito.Mockito.verify;

@Slf4j
@SpringBootTest(classes = DeptRouter.class)
public class DeptRouterTest {

   @Autowired
   private DeptRouter deptRouter;

   @MockitoBean
   protected UserProvisioning userProvisioning;

   @MockitoBean
   private CourseProvisioning courseProvisioning;

   @MockitoBean
   private EnrollmentProvisioning enrollmentProvisioning;

   @MockitoBean
   private SectionProvisioning sectionProvisioning;

   @MockitoBean
   private ExpandEnrollmentProvisioning expandEnrollmentProvisioning;

   @MockitoBean
   private CanvasImportIdRepository canvasImportIdRepository;

   @MockitoBean
   private ImportService importService;

   @MockitoBean
   private EmailService emailService;

   @Captor
   ArgumentCaptor<EmailDetails> emailCaptor;

   @MockitoBean
   private CsvService csvService;

   @MockitoBean
   private BatchEmailServiceImpl batchEmailService;

   @MockitoBean
   private CanvasService canvasService;

   @MockitoBean
   private ArchiveRepository archiveRepository;

   @Test
   public void testResultsEmailUsesHostNotBaseUrl() throws Exception {
      final ProvisioningResult.FileObject fileObject = new ProvisioningResult.FileObject("file1", new byte[1]);
      final StringBuilder emailMessage = new StringBuilder();
      emailMessage.append("Test email message\n");

      final LmsBatchEmail lmsBatchEmail = new LmsBatchEmail();
      lmsBatchEmail.setEmails("me@iu.edu");

      Mockito.when(batchEmailService.getBatchEmailFromGroupCode("dept1")).thenReturn(lmsBatchEmail);
      Mockito.when(emailService.getStandardHeader()).thenReturn("HEADER");

      Mockito.when(canvasService.getHost()).thenReturn("MYHOST");
      Mockito.when(canvasService.getBaseUrl()).thenReturn("https://MYHOST");


      final String importId = deptRouter.sendToCanvas(new ArrayList<ProvisioningResult.FileObject>(), "dept1",
              emailMessage, 1L, "user1", Constants.SOURCE.APP);

      verify(emailService).sendEmail(emailCaptor.capture());

      EmailDetails emailDetails = emailCaptor.getValue();
      Assertions.assertNotNull(emailDetails);

      final String body = emailDetails.getBody();
      Assertions.assertNotNull(body);

      Assertions.assertFalse(body.contains("The final results from Canvas (https://MYHOST) will be sent out at a later time"));
      Assertions.assertTrue(body.contains("The final results from Canvas (MYHOST) will be sent out at a later time"));
   }
}
