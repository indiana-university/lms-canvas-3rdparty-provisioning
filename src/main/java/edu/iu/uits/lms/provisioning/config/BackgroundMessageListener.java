package edu.iu.uits.lms.provisioning.config;

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
import email.client.generated.api.EmailApi;
import email.client.generated.model.EmailDetails;
import iuonly.client.generated.api.BatchEmailApi;
import iuonly.client.generated.model.LmsBatchEmail;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.collections4.MultiValuedMap;
import org.apache.commons.collections4.multimap.ArrayListValuedHashMap;
import org.springframework.amqp.rabbit.annotation.RabbitHandler;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

@RabbitListener(queues = "${deptprov.backgroundQueueName}")
@Profile("!batch")
@Component
@Slf4j
public class BackgroundMessageListener {

   @Autowired
   private DeptRouter deptRouter;

   @Autowired
   private CanvasImportIdRepository canvasImportIdRepository;

   @Autowired
   private EmailApi emailApi;

   @Autowired
   private BatchEmailApi batchEmailApi;

   @RabbitHandler
   public void receive(BackgroundMessage message) {
      log.info("Received <{}>", message);
      MultiValuedMap<DeptRouter.CSV_TYPES, FileContent> postProcessingDataMap = new ArrayListValuedHashMap<>();

      try {
         List<ProvisioningResult> provisioningResults = deptRouter.processFiles(message.getDepartment(), message.getFilesByType(), message.getNotificationForm());

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
      LmsBatchEmail emails = batchEmailApi.getBatchEmailFromGroupCode(dept);
      String[] emailAddresses = null;
      if (emails != null) {
         emailAddresses = emails.getEmails().split(",");
      }
      if (emailAddresses != null && emailAddresses.length > 0) {
         // create the subject line of the email
         String subject = emailApi.getStandardHeader() + " processing errors for " + dept;

         StringBuilder finalMessage = new StringBuilder();
         finalMessage.append("The preprocessing stage of your Canvas provisioning job is incomplete and encountered the following errors below.\r\n\r\n");

         for (FileError fe : pe.getFileErrors()) {
            finalMessage.append(fe.getTitle() + "\r\n");
            finalMessage.append("\t" + fe.getDescription() + "\r\n");
         }

         // email has been combined together, so send it!
         EmailDetails details = new EmailDetails();
         details.setRecipients(Arrays.asList(emailAddresses));
         details.setSubject(subject);
         details.setBody(finalMessage.toString());
         emailApi.sendEmail(details, true);
      }
   }

}
