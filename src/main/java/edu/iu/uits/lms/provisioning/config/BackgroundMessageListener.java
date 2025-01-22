package edu.iu.uits.lms.provisioning.config;

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

import com.rabbitmq.client.Channel;
import edu.iu.uits.lms.email.model.EmailDetails;
import edu.iu.uits.lms.email.service.EmailService;
import edu.iu.uits.lms.email.service.LmsEmailTooBigException;
import edu.iu.uits.lms.iuonly.model.LmsBatchEmail;
import edu.iu.uits.lms.iuonly.model.acl.AuthorizedUser;
import edu.iu.uits.lms.iuonly.services.AuthorizedUserService;
import edu.iu.uits.lms.iuonly.services.BatchEmailServiceImpl;
import edu.iu.uits.lms.provisioning.Constants;
import edu.iu.uits.lms.provisioning.model.CanvasImportId;
import edu.iu.uits.lms.provisioning.model.PostProcessingData;
import edu.iu.uits.lms.provisioning.model.content.FileContent;
import edu.iu.uits.lms.provisioning.repository.CanvasImportIdRepository;
import edu.iu.uits.lms.provisioning.service.DeptRouter;
import edu.iu.uits.lms.provisioning.service.ProvisioningResult;
import edu.iu.uits.lms.provisioning.service.exception.FileError;
import edu.iu.uits.lms.provisioning.service.exception.FileProcessingException;
import edu.iu.uits.lms.provisioning.service.exception.FileUploadException;
import edu.iu.uits.lms.provisioning.service.exception.ProvisioningException;
import edu.iu.uits.lms.provisioning.service.exception.ZipException;
import jakarta.mail.MessagingException;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.collections4.MultiValuedMap;
import org.apache.commons.collections4.multimap.ArrayListValuedHashMap;
import org.springframework.amqp.rabbit.annotation.RabbitHandler;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.amqp.support.AmqpHeaders;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Profile;
import org.springframework.messaging.handler.annotation.Header;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import static edu.iu.uits.lms.provisioning.Constants.AUTH_USER_TOOL_PERMISSION;
import static edu.iu.uits.lms.provisioning.Constants.AUTH_USER_TOOL_PERM_PROP_ALLOW_SIS_ENROLLMENTS;
import static edu.iu.uits.lms.provisioning.Constants.AUTH_USER_TOOL_PERM_PROP_AUTHORIZED_ACCOUNTS;
import static edu.iu.uits.lms.provisioning.Constants.AUTH_USER_TOOL_PERM_PROP_OVERRIDE_RESTRICTIONS;

@RabbitListener(queues = "${deptprov.backgroundQueueName}")
@Profile("!batch")
@Component
@Slf4j
public class BackgroundMessageListener {

   @Autowired
   private DeptRouter deptRouter;

   @Autowired
   private AuthorizedUserService authorizedUserService;

   @Autowired
   private CanvasImportIdRepository canvasImportIdRepository;

   @Autowired
   private EmailService emailService;

   @Autowired
   private BatchEmailServiceImpl batchEmailService;

   @RabbitHandler
   public void receive(BackgroundMessage message, Channel channel, @Header(AmqpHeaders.DELIVERY_TAG) long deliveryTag) {
      log.info("Received <{}>", message);

      try {
         // ack the message
         channel.basicAck(deliveryTag, false);

         // do the message stuff!
         handleMessage(message);
      } catch (IOException e) {
         log.error("unable to ack the message from the queue", e);
      }
   }

   public void handleMessage(BackgroundMessage message) {
      MultiValuedMap<DeptRouter.CSV_TYPES, FileContent> postProcessingDataMap = new ArrayListValuedHashMap<>();

      AuthorizedUser user = authorizedUserService.findByUsernameAndToolPermission(message.getUsername(), Constants.AUTH_USER_TOOL_PERMISSION);
      Map<String, String> propertyMap = user.getToolPermissionProperties(AUTH_USER_TOOL_PERMISSION);

      boolean allowSisEnrollments = AuthorizedUserService.convertPropertyToBoolean(propertyMap.get(AUTH_USER_TOOL_PERM_PROP_ALLOW_SIS_ENROLLMENTS));
      boolean overrideRestrictions = AuthorizedUserService.convertPropertyToBoolean(propertyMap.get(AUTH_USER_TOOL_PERM_PROP_OVERRIDE_RESTRICTIONS));
      List<String> authorizedAccounts = AuthorizedUserService.convertPropertyToList(propertyMap.get(AUTH_USER_TOOL_PERM_PROP_AUTHORIZED_ACCOUNTS));

      try {
         List<ProvisioningResult> provisioningResults = deptRouter.processFiles(message.getDepartment(), message.getFilesByType(),
                 message.getNotificationForm(), allowSisEnrollments, authorizedAccounts, overrideRestrictions);

         List<ProvisioningResult.FileObject> allFiles = new ArrayList<>();
         StringBuilder fullEmail = new StringBuilder();
         for (ProvisioningResult provisioningResult : provisioningResults) {
            ProvisioningResult.FileObject fileObject = provisioningResult.getFileObject();
            if (fileObject != null) {
               allFiles.add(fileObject);
            }
            if (provisioningResult.getDeferredProcessingData() != null) {
               postProcessingDataMap.put(provisioningResult.getDeferredProcessingDataType(), provisioningResult.getDeferredProcessingData());
            }
            fullEmail.append(provisioningResult.getEmailMessage() + "\r\n");
         }

         String importId = deptRouter.sendToCanvas(allFiles, message.getDepartment(), fullEmail, message.getArchiveId(), message.getUsername(), message.getSource());
         if (importId != null && !"".equals(importId)) {
            if (!postProcessingDataMap.isEmpty()) {
               log.debug("{}", postProcessingDataMap);
               CanvasImportId canvasImport = canvasImportIdRepository.findById(importId).orElse(null);
               if (canvasImport != null) {
                  canvasImport.setPostProcessingData(new PostProcessingData(postProcessingDataMap));
                  canvasImportIdRepository.save(canvasImport);
               }
            }
         }
      } catch (FileProcessingException | FileUploadException | ZipException e) {
         log.error("Error processing provisioning files", e);
         sendEmail(message.getDepartment(), e);
      }
   }

   /**
    * Send an email with error information
    * @param dept
    * @param pe
    */
   private void sendEmail(String dept, ProvisioningException pe) {
      LmsBatchEmail emails = batchEmailService.getBatchEmailFromGroupCode(dept);
      String[] emailAddresses = null;
      if (emails != null) {
         emailAddresses = emails.getEmails().split(",");
      }
      if (emailAddresses != null && emailAddresses.length > 0) {
         // create the subject line of the email
         String subject = emailService.getStandardHeader() + " processing errors for " + dept;

         StringBuilder finalMessage = new StringBuilder();
         finalMessage.append("The preprocessing stage of your Canvas provisioning job is incomplete and encountered the following errors below.\r\n\r\n");

         for (FileError fe : pe.getFileErrors()) {
            finalMessage.append(fe.getTitle() + "\r\n");
            finalMessage.append("\t" + fe.getDescription() + "\r\n");
         }

         // email has been combined together, so send it!
         EmailDetails details = new EmailDetails();
         details.setRecipients(emailAddresses);
         details.setSubject(subject);
         details.setBody(finalMessage.toString());
         try {
            emailService.sendEmail(details);
         } catch (LmsEmailTooBigException | MessagingException e) {
            log.error("Unable to send email", e);
         }
      }
   }

}
