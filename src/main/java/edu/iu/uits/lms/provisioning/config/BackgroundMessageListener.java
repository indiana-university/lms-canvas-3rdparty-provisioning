package edu.iu.uits.lms.provisioning.config;

import com.rabbitmq.client.Channel;
import edu.iu.uits.lms.email.model.EmailDetails;
import edu.iu.uits.lms.email.service.EmailService;
import edu.iu.uits.lms.email.service.LmsEmailTooBigException;
import edu.iu.uits.lms.iuonly.model.DeptProvisioningUser;
import edu.iu.uits.lms.iuonly.model.LmsBatchEmail;
import edu.iu.uits.lms.iuonly.services.BatchEmailServiceImpl;
import edu.iu.uits.lms.iuonly.services.DeptProvisioningUserServiceImpl;
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

import javax.mail.MessagingException;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

@RabbitListener(queues = "${deptprov.backgroundQueueName}")
@Profile("!batch")
@Component
@Slf4j
public class BackgroundMessageListener {

   @Autowired
   private DeptRouter deptRouter;

   @Autowired
   private DeptProvisioningUserServiceImpl deptProvisioningUserApi;

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

      DeptProvisioningUser user = deptProvisioningUserApi.findByUsername(message.getUsername());
      boolean allowSisEnrollments = user.isAllowSisEnrollments();
      boolean overrideRestrictions = user.isOverrideRestrictions();
      List<String> authorizedAccounts = new ArrayList<>();
      String authorizedAccountString = user.getAuthorizedAccounts();

      if (authorizedAccountString != null) {
         authorizedAccounts = Arrays.stream(authorizedAccountString.split(","))
                 .map(String::trim)
                 .collect(Collectors.toList());
      }

      try {
         List<ProvisioningResult> provisioningResults = deptRouter.processFiles(message.getDepartment(), message.getFilesByType(), message.getNotificationForm(), allowSisEnrollments, authorizedAccounts, overrideRestrictions);

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
            emailService.sendEmail(details, true);
         } catch (LmsEmailTooBigException | MessagingException e) {
            log.error("Unable to send email", e);
         }
      }
   }

}
