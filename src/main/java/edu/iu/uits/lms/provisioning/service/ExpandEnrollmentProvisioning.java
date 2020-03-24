package edu.iu.uits.lms.provisioning.service;

import canvas.client.generated.api.ExpandApi;
import canvas.client.generated.api.UsersApi;
import canvas.client.generated.model.ExpandEnrollment;
import canvas.client.generated.model.User;
import edu.iu.uits.lms.provisioning.model.content.FileContent;
import edu.iu.uits.lms.provisioning.model.content.StringArrayFileContent;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.extern.log4j.Log4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;

@Log4j
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
    public List<ProvisioningResult> processEnrollments(Collection<FileContent> fileToProcess) {
        List<ProvisioningResult> prs = new ArrayList<>();
        for (FileContent file : fileToProcess) {
            StringBuilder emailMessage = processInputFiles((StringArrayFileContent) file);
            prs.add(new ProvisioningResult(emailMessage, null, false));
        }
        return prs;
    }

    /**
     * @param fileToProcess
     * @return
     */
    private StringBuilder processInputFiles(StringArrayFileContent fileToProcess) {
        StringBuilder emailMessage = new StringBuilder(fileToProcess.getFileName() + ":\r\n");

//        List<ExpandEnrollment> enrollmentList = new ArrayList<>();

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

                    ExpandEnrollment ee = new ExpandEnrollment();
                    ee.setCanvasUserId(userId);
                    ee.setListingId(listingId);
                    processEnrollment(ee, emailMessage, processCounts);
//                    enrollmentList.add(ee);
                }
            }
        }

//        EnrollmentMessage enrollmentMessage = new EnrollmentMessage(fileToProcess.getName(), dept, enrollmentList);
//        jmsService.objectSend(enrollmentMessage);
        return emailMessage;
    }

    private void processEnrollment(ExpandEnrollment enrollment, StringBuilder emailMessage, ProcessCounts processCounts) {
        // from the message population from csv we dont know what the user id is.
        // what comes in right now is either a sis_id or a login_id.  This model object
        // is reused from the actual expand create REST call hence why this field is called
        // canvas_user_id as at that point this is what this is
        String userId = enrollment.getCanvasUserId();

        User user = null;

        final String originalUserId = enrollment.getCanvasUserId();


        // Normally we'd use th elogic of if a number assume a sisid, otherwise assume a login id
        // But since Lynn's guest account (lynnward40@yahoo.com) they set it up backwards
        // and functional wants to cover this scenerio we added an "opposite" check to cover
        // this after the initial call fails to find any user
        if (userId.matches("[0-9]+")) {
            // get canvasId for user by sis id
            log.debug("Looking up user " + userId + " by sisId");

            user = usersApi.getUserBySisId(userId);

            if (user == null) {
                log.debug("Not found by sis id. Trying login id");
                user = usersApi.getUserBySisLoginId(userId);
            }
        } else { // must be login id
            // get canvasId for user by login id
            log.debug("Looking up user " + userId + " by sisLoginId");

            user = usersApi.getUserBySisLoginId(userId);

            if (user == null) {
                log.debug("Not found by login id. Trying sis id");
                user = usersApi.getUserBySisId(userId);
            }
        }

        if (user != null) {
            enrollment.setCanvasUserId(user.getId());

            log.debug("found user = " + user.getName() + ", canvas id = " + user.getId());

            if (expandApi.addUserToListing(enrollment)) {
                processCounts.incrementSuccessCount();
            } else {
                processCounts.incrementFailureCount();
                emailMessage.append("- Could not add enrollment for the csv supplied user id " + originalUserId
                      + ", listingId " + enrollment.getListingId() + "\r\n");
            }

        } else {
            log.error("could not find user for " + userId);
            emailMessage.append("- Could not find the canvas user identified by the csv supplied user id " + originalUserId + "\r\n");
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
