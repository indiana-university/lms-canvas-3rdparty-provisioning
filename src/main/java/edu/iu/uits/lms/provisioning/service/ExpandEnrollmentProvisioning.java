package edu.iu.uits.lms.provisioning.service;

import canvas.client.generated.api.ExpandApi;
import canvas.client.generated.api.UsersApi;
import canvas.client.generated.model.User;
import edu.iu.uits.lms.provisioning.model.content.FileContent;
import edu.iu.uits.lms.provisioning.model.content.StringArrayFileContent;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;

@Slf4j
@Service
public class ExpandEnrollmentProvisioning {

    @Autowired
    private UsersApi usersApi;

    @Autowired
    private ExpandApi expandApi;

    /**
     * Pass in a path to a csv file and this will validate the data and send enrollments to Expand
     * @param fileToProcess
     */
    public List<ProvisioningResult> processEnrollments(Collection<FileContent> fileToProcess, boolean deferredProcessing) {
        List<ProvisioningResult> prs = new ArrayList<>();
        if (deferredProcessing) {
            if (fileToProcess.size() > 0) {
                log.debug("Deferring expand enrollment provisioning");
                StringBuilder emailMessage = new StringBuilder("Processing of the following files has been deferred until after all other files have been imported into Canvas:\r\n");
                for (FileContent file : fileToProcess) {
                    emailMessage.append("\t").append(file.getFileName()).append("\r\n");
                    prs.add(new ProvisioningResult(emailMessage, null, false, file, DeptRouter.CSV_TYPES.EXPAND_ENROLLMENTS));
                }
            }
        } else {
            for (FileContent file : fileToProcess) {
                StringBuilder emailMessage = processInputFiles((StringArrayFileContent) file);
                prs.add(new ProvisioningResult(emailMessage, null, false));
            }
        }
        return prs;
    }

    /**
     * @param fileToProcess
     * @return
     */
    private StringBuilder processInputFiles(StringArrayFileContent fileToProcess) {
        StringBuilder emailMessage = new StringBuilder(fileToProcess.getFileName() + ":\r\n");

        // read individual files line by line
        List<String[]> fileContents = fileToProcess.getContents();
        ProcessCounts processCounts = new ProcessCounts();

        for (String[] lineContentArray : fileContents) {
            int lineLength = lineContentArray.length;

            if (lineLength == 2) {
                // check to see if this is a header line
                // if it's a header row, we want to ignore it and move on
                String[] expandEnrollmentsHeader = CsvService.EXPAND_ENROLLMENT_HEADER.split(",");

                if (!Arrays.equals(lineContentArray, expandEnrollmentsHeader)) {

                    String userId = lineContentArray[0];
                    String listingId = lineContentArray[1];

                    processEnrollment(userId, listingId, emailMessage, processCounts);
                }
            }
        }

        emailMessage.append("\tUsers failed to be added to expand's enrollment: " + processCounts.getFailureCount() + "\r\n");
        emailMessage.append("\tUsers successfully added to expand's enrollment: " + processCounts.getSuccessCount() + "\r\n");
        emailMessage.append("\tTotal records processed: " + processCounts.getTotalCount() + "\r\n");

        return emailMessage;
    }

    private void processEnrollment(String canvasUserId, String listingId, StringBuilder emailMessage, ProcessCounts processCounts) {
        // from the message population from csv we don't know what the user id is.
        // what comes in right now is either a sis_id or a login_id.  This model object
        // is reused from the actual expand create REST call hence why this field is called
        // canvas_user_id as at that point this is what this is

        User user = null;

        // Normally we'd use the logic of if a number assume a sisid, otherwise assume a login id
        // But since Lynn's guest account (lynnward40@yahoo.com) they set it up backwards
        // and functional wants to cover this scenario we added an "opposite" check to cover
        // this after the initial call fails to find any user
        if (canvasUserId.matches("[0-9]+")) {
            // get canvasId for user by sis id
            log.debug("Looking up user " + canvasUserId + " by sisId");

            user = usersApi.getUserBySisId(canvasUserId);

            if (user == null) {
                log.debug("Not found by sis id. Trying login id");
                user = usersApi.getUserBySisLoginId(canvasUserId);
            }
        } else { // must be login id
            // get canvasId for user by login id
            log.debug("Looking up user " + canvasUserId + " by sisLoginId");

            user = usersApi.getUserBySisLoginId(canvasUserId);

            if (user == null) {
                log.debug("Not found by login id. Trying sis id");
                user = usersApi.getUserBySisId(canvasUserId);
            }
        }

        if (user != null) {
            log.debug("found user = " + user.getName() + ", canvas id = " + user.getId());

            if (expandApi.addUserToListing(user.getId(), listingId)) {
                processCounts.incrementSuccessCount();
            } else {
                processCounts.incrementFailureCount();
                emailMessage.append("\tCould not add enrollment for the csv supplied user id " + canvasUserId
                      + ", listingId " + listingId + "\r\n");
            }

        } else {
            log.error("could not find user for " + canvasUserId);
            emailMessage.append("\tCould not find the canvas user identified by the csv supplied user id " + canvasUserId + "\r\n");
            processCounts.incrementFailureCount();
        }

    }

    @Data
    @NoArgsConstructor
    private static class ProcessCounts {
        private int successCount = 0;
        private int failureCount = 0;
        private int totalCount = 0;

        public void incrementSuccessCount() {
            this.successCount++;
            this.totalCount++;
        }

        public void incrementFailureCount() {
            this.failureCount++;
            this.totalCount++;
        }
    }
}
