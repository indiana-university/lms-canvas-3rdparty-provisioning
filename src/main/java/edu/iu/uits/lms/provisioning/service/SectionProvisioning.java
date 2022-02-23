package edu.iu.uits.lms.provisioning.service;

import canvas.client.generated.api.AccountsApi;
import canvas.client.generated.api.CoursesApi;
import canvas.client.generated.model.Account;
import canvas.client.generated.model.Course;
import edu.iu.uits.lms.provisioning.model.content.FileContent;
import edu.iu.uits.lms.provisioning.model.content.StringArrayFileContent;
import iuonly.client.generated.api.SudsApi;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Slf4j
@Service
public class SectionProvisioning {

   @Autowired
   private AccountsApi accountsApi;

   @Autowired
   private CoursesApi coursesApi;

   @Autowired
   private CsvService csvService;

   @Autowired
   private SudsApi sudsApi;

   public List<ProvisioningResult> processSections(Collection<FileContent> fileToProcess, List<String> authorizedAccounts, boolean overrideRestrictions) {
      List<ProvisioningResult> prs = new ArrayList<>();
      StringBuilder emailMessage = new StringBuilder();
      StringBuilder errorMessage = new StringBuilder();
      List<String[]> stringArrayList = new ArrayList<>();
      Map<String, Boolean> sisCourses = new HashMap<>();
      Map<String, Boolean> sisSections = new HashMap<>();
      Map<String, Boolean> accountChecksMap = new HashMap<>();

      for (FileContent file : fileToProcess) {
         // read individual files line by line
         List <String[]> fileContents = ((StringArrayFileContent)file).getContents();
         emailMessage.append(file.getFileName() + ":\r\n");
         int rowCounter = 0;
         int headerLength = 0;

         for (String[] lineContentArray : fileContents) {
            // increment the counter here and not several places
            rowCounter++;

            if (rowCounter == 1) {
               headerLength = lineContentArray.length;
               stringArrayList.add(lineContentArray);
               continue;
            }

            int lineLength = lineContentArray.length;

            if (lineLength != headerLength) {
               errorMessage.append("\tLine " + rowCounter + " did not match the amount of fields specified in the header. Skipping. Double check the amount of commas and try again.\r\n");
               continue;
            }

            // if the user has override permissions, do not worry about the other lookups and move on!
            if (overrideRestrictions) {
               // Everything is cool, add it in!
               log.debug("Successfully added in row " + rowCounter);
               stringArrayList.add(lineContentArray);
               continue;
            }

            // sectionId is always the first entry in the section.csv
            String sectionId = lineContentArray[0];

            // courseId is always the second entry in the section.csv
            String courseId = lineContentArray[1];

            // if overrideRestrictions OR allowSis is true, skip the SIS checks
            boolean doSisCheck = true;

            if (sisCourses.containsKey(courseId)) {
               if (sisCourses.get(courseId)) {
                  // confirmed course is 'true' in the map, so skip the sis course lookup later
                  log.debug("Skipped the course " + courseId + " check because we already verified it as good.");
                  doSisCheck = false;
               } else {
                  // confirmed course is 'false', so skip this line since we know it's not ok to use
                  log.debug("Skipped " + rowCounter + " because it is a SIS course and we already checked.");
                  errorMessage.append("\tLine " + rowCounter + " is a SIS course and your account is not allowed to make changes to SIS courses.\r\n");
                  continue;
               }
            }

            if (sisSections.containsKey(sectionId)) {
               if (sisSections.get(sectionId)) {
                  // confirmed section is 'true' in the map, so skip the suds lookup later
                  log.debug("Skipped the sectionId " + sectionId + " check because we already verified it as good.");
                  doSisCheck = false;
               } else {
                  // confirmed section is 'false', so skip this line since we know it's not ok to use
                  log.debug("Skipped " + rowCounter + " because it is a SIS section and we already checked.");
                  errorMessage.append("\tLine " + rowCounter + " is a SIS section and your account is not allowed to make changes to SIS sections.\r\n");
                  continue;
               }
            }

            if (doSisCheck) {
               // look up for SIS stuff
               if (sudsApi.getSudsCourseBySiteId(courseId) != null) {
                  log.debug("Skipped " + rowCounter + " because it is a SIS course and user did not have SIS permission.");
                  errorMessage.append("\tLine " + rowCounter + " is a SIS course and your account is not allowed to make changes to SIS courses.\r\n");
                  sisCourses.put(courseId, false);
                  continue;
               } else if (sudsApi.getSudsArchiveCourseBySiteId(courseId) != null) {
                  log.debug("Skipped " + rowCounter + " because it is an archived SIS course and user did not have SIS permission.");
                  errorMessage.append("\tLine " + rowCounter + " is a SIS course and your account is not allowed to make changes to SIS courses.\r\n");
                  sisCourses.put(courseId, false);
                  continue;
               }

               // look up for SIS stuff
               if (sudsApi.getSudsCourseBySiteId(sectionId) != null) {
                  log.debug("Skipped " + rowCounter + " because it is a SIS section and user did not have SIS permission.");
                  errorMessage.append("\tLine " + rowCounter + " is a SIS section and your account is not allowed to make changes to SIS sections.\r\n");
                  sisSections.put(sectionId, false);
                  continue;
               } else if (sudsApi.getSudsArchiveCourseBySiteId(sectionId) != null) {
                  log.debug("Skipped " + rowCounter + " because it is an archived SIS section and user did not have SIS permission.");
                  errorMessage.append("\tLine " + rowCounter + " is a SIS section and your account is not allowed to make changes to SIS sections.\r\n");
                  sisSections.put(sectionId, false);
                  continue;
               }
            }

            // if we made it here, it passed the SIS checks. Add it to the lists
            sisCourses.put(courseId, true);
            sisSections.put(sectionId, true);

            boolean isAccountAuthorized = false;

            if (authorizedAccounts.contains("ALL")) {
               // have authority for all nodes, so set the boolean
               isAccountAuthorized = true;
            } else {
               Course course = coursesApi.getCourse("sis_course_id:" + courseId);
               if (course == null) {
                  // course does not exist in Canvas yet, so give this a pass and assume it's cool
                  isAccountAuthorized = true;
               } else {
                  Account courseAccount = accountsApi.getAccount(course.getAccountId());
                  if (authorizedAccounts.contains(courseAccount.getName())) {
                     isAccountAuthorized = true;
                  } else {
                     // get the parent account names
                     List<String> parentAccountNames = accountsApi.getParentAccounts(courseAccount.getId())
                             .stream().map(parentNames -> parentNames.getName()).collect(Collectors.toList());

                     for (String authorizedAccount : authorizedAccounts) {
                        if (parentAccountNames.contains(authorizedAccount)) {
                           isAccountAuthorized = true;
                           break;
                        }
                     }
                  }
               }
            }

            if (isAccountAuthorized) {
               // Everything is cool, add it in!
               log.debug("Successfully added in row " + rowCounter);
               stringArrayList.add(lineContentArray);
               continue;
            } else {
               log.debug("Skipped " + rowCounter + " because user is not authorized to provision to this account.");
               errorMessage.append("\tLine " + rowCounter + " is in a node that your account is not allowed to make changes.\r\n");
               continue;
            }
         }

         StringBuilder finalMessage = new StringBuilder(emailMessage);
         byte[] fileBytes = null;
         boolean fileException = false;
         try {
            // Create csv file to send to Canvas
            fileBytes = csvService.writeCsvToBytes(stringArrayList, null);

            if (errorMessage.length() > 0) {
               finalMessage.append(errorMessage);
               finalMessage.append("\tAll other entries were sent to Canvas.\r\n");
            } else {
               finalMessage.append("\tAll entries were sent to Canvas.\r\n");
            }
         } catch (IOException e) {
            log.error("Error generating csv", e);
            finalMessage.append("\tThere were errors when generating the CSV file to send to Canvas\r\n");
            fileException = true;
         }

            ProvisioningResult pr = new ProvisioningResult(finalMessage,
                  new ProvisioningResult.FileObject(file.getFileName(), fileBytes), fileException);
            prs.add(pr);
      }

      return prs;
   }
}
