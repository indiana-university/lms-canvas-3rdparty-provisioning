package edu.iu.uits.lms.provisioning.service;

import canvas.client.generated.api.CanvasApi;
import canvas.client.generated.api.ImportApi;
import canvas.client.generated.model.CanvasUploadStatus;
import edu.iu.uits.lms.provisioning.config.ToolConfig;
import edu.iu.uits.lms.provisioning.model.CanvasImportId;
import edu.iu.uits.lms.provisioning.model.content.FileContent;
import edu.iu.uits.lms.provisioning.repository.CanvasImportIdRepository;
import edu.iu.uits.lms.provisioning.service.exception.FileProcessingException;
import email.client.generated.api.EmailApi;
import email.client.generated.model.EmailDetails;
import iuonly.client.generated.api.BatchEmailApi;
import iuonly.client.generated.model.LmsBatchEmail;
import lombok.Getter;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.collections4.MultiValuedMap;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Service
@Slf4j
public class EmailSummaryService {

   @Autowired
   private CanvasImportIdRepository canvasImportIdRepository;

   @Autowired
   private EmailApi emailApi;

   @Autowired
   private BatchEmailApi batchEmailApi;

   @Autowired
   private ImportApi importApi;

   @Autowired
   private CanvasApi canvasApi;

   @Autowired
   private DeptRouter deptRouter;

   @Autowired
   private ToolConfig toolConfig;

   public void processResults() {
      Map<String, CanvasImportObject> importTrackerMap = new HashMap<>();
      Map<String, MultiValuedMap<DeptRouter.CSV_TYPES, FileContent>> postProcessingMap = new HashMap<>();

      List<CanvasImportId> results = canvasImportIdRepository.findByProcessedOrderByGroupCodeAscImportIdAsc("N");

      for (CanvasImportId item : results) {
         String groupCode = item.getGroupCode();
         String importId = item.getImportId();
         CanvasImportObject cio = importTrackerMap.get(groupCode);
         if (cio != null) {
            cio.addImportId(importId);
         }
         else {
            cio = new CanvasImportObject();
            List<String> importIdList = new ArrayList<>();
            cio.setGroupCode(groupCode);
            importIdList.add(importId);
            cio.setImportIdsList(importIdList);
            cio.setEmailMessage(new StringBuilder());
            importTrackerMap.put(groupCode, cio);
         }
         if (item.getPostProcessingData() != null) {
            postProcessingMap.put(importId, item.getPostProcessingData().getPostProcessingDataMap());
         }
      }

      // if the list is empty, don't bother continuing
      if (importTrackerMap.isEmpty()) {
         log.info("There were no importIds to process. Exiting gracefully.");
         return;
      }

      List<String> processedImportIds = new ArrayList<String>();
      // parse out the warnings
      for (CanvasImportObject cio : importTrackerMap.values()) {
         //Setting a default email
         cio.setEmailList(Collections.singletonList(toolConfig.getDefaultBatchNotificationEmail()));
         LmsBatchEmail batchEmail = batchEmailApi.getBatchEmailFromGroupCode(cio.getGroupCode());
         if (batchEmail != null && batchEmail.getEmails() != null) {
            String[] emailAddresses = batchEmail.getEmails().split(",");
            if (emailAddresses.length > 0) {
               //Set the actual email addresses, if any
               cio.setEmailList(Arrays.asList(emailAddresses));
            }
         }

         // if we made it here, then we got some ids to look up in Canvas!
         List<String> importIdList = cio.getImportIdsList();
         StringBuilder emailMessage = cio.getEmailMessage();

         log.debug("Import IDs for {}: {}", cio.getGroupCode(), importIdList);

         for (String importId : importIdList) {
            processImport(importId, emailMessage, postProcessingMap, cio, processedImportIds);
         }

         // check if there are warnings, if so, send an email
         if (emailMessage.length() > 0) {
            String subject = emailApi.getStandardHeader() + " " + cio.getGroupCode() + " Canvas Provisioning Errors";

            EmailDetails emailDetails = new EmailDetails();
            emailDetails.setRecipients(cio.getEmailList());
            emailDetails.setSubject(subject);
            emailDetails.setBody(emailMessage.toString());
            emailApi.sendEmail(emailDetails, true);
         } else {
            log.info("There were no warnings from the Canvas imports.  Update the records as processed and will not send an email.");
         }
      }

      // Update the records of importIds we processed
      for (String importId : processedImportIds) {
         updateImportIdRecords(importId);
      }
   }

   protected void processImport(String importId, StringBuilder emailMessage, Map<String,
         MultiValuedMap<DeptRouter.CSV_TYPES, FileContent>> postProcessingMap, CanvasImportObject cio,
                                  List<String> processedImportIds) {
      CanvasUploadStatus importStatus = importApi.getImportStatus(importId);
      // a call to see if the import for this id is finished
      boolean canvasFinished = false;
      String endedAt = importStatus.getEndedAt();
      if (endedAt != null && !"".equals(endedAt) && !"null".equals(endedAt)) {
         // this import is done, so set the boolean to true
         canvasFinished = true;
      }

      // if Canvas is done processing, check for errors
      if (canvasFinished) {
         List<List<String>> canvasWarningsList = importStatus.getProcessingWarnings();
         List<List<String>> canvasErrorsList = importStatus.getProcessingErrors();

         if (emailMessage.length() == 0) {
            emailMessage.append("Here is a report of errors from Canvas (" + canvasApi.getBaseUrl() + "):\r\n\r\n");
         }

         emailMessage.append("From importId: " + importId + "\r\n\r\n");
         if (canvasErrorsList != null || canvasWarningsList != null) {
            if (canvasErrorsList != null) {
               for (List<String> canvasErrors : canvasErrorsList) {
                  for (String canvasError : canvasErrors) {
                     if (canvasError != null) {
                        if (canvasError.contains(".csv")) {
                           emailMessage.append(canvasError + " - ");
                        } else {
                           emailMessage.append(canvasError + "\r\n");
                        }
                     }
                  }
               }
            }
            if (canvasWarningsList != null) {
               for (List<String> canvasWarnings : canvasWarningsList) {
                  for (String canvasWarning : canvasWarnings) {
                     if (canvasWarning.contains(".csv")) {
                        emailMessage.append(canvasWarning + " - ");
                     } else {
                        emailMessage.append(canvasWarning + "\r\n");
                     }
                  }
               }
            }
         } else {
            emailMessage.append("No errors/warnings\r\n");
         }

         // Do post processing stuff
         MultiValuedMap<DeptRouter.CSV_TYPES, FileContent> postProcessingData = postProcessingMap.get(importId);
         if (postProcessingData != null) {
            emailMessage.append("\r\nPost processing results:\r\n");
            try {
               List<ProvisioningResult> provisioningResults = deptRouter.processFiles(cio.getGroupCode(), postProcessingData, null, true, null, true);
               for (ProvisioningResult provisioningResult : provisioningResults) {
                  emailMessage.append(provisioningResult.getEmailMessage() + "\r\n");
               }
            } catch (FileProcessingException e) {
               log.error("Error trying to post process", e);
               emailMessage.append("\tThere were errors with the post processing:\r\n");
               emailMessage.append("\t\t" + e.getFileErrors() + "\r\n");
            }
         }
         emailMessage.append("\r\n");

         processedImportIds.add(importId);
      }
   }

   private void updateImportIdRecords(String importId) {
      canvasImportIdRepository.setProcessedByImportId("Y", importId, new Date());
      log.info("Updated the record as 'processed' for importId: " + importId);
   }

   @Setter
   @Getter
   protected static class CanvasImportObject {
      private List<String> emailList;
      private StringBuilder emailMessage;
      private String groupCode;
      private List<String> importIdsList;

      /**
       * Convenience method to add an importId to the importIdsList
       */
      public void addImportId(String importId) {
         this.importIdsList.add(importId);
      }
   }
}
