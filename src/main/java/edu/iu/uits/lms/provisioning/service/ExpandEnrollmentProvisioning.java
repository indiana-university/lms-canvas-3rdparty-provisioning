package edu.iu.uits.lms.provisioning.service;

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

import edu.iu.uits.lms.canvas.model.User;
import edu.iu.uits.lms.canvas.services.CatalogListingService;
import edu.iu.uits.lms.canvas.services.UserService;
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
    private UserService userService;

    @Autowired
    private CatalogListingService catalogListingService;

    /**
     * Pass in a path to a csv file and this will validate the data and send enrollments to Expand
     * @param fileToProcess List of files to process
     * @param deferredProcessing Flag indicating of processing should be deferred
     * @return List of ProvisioningResult objects
     */
    public List<ProvisioningResult> processEnrollments(Collection<FileContent> fileToProcess, boolean deferredProcessing) {
        List<ProvisioningResult> prs = new ArrayList<>();
        if (deferredProcessing) {
            if (fileToProcess.size() > 0) {
                log.debug("Deferring expand enrollment provisioning");
                for (FileContent file : fileToProcess) {
                    StringBuilder emailMessage = new StringBuilder(file.getFileName())
                          .append(":\r\n\tProcessing of this file has been deferred until after all other files have been imported into Canvas\r\n");
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
     * Process the content
     * @param fileToProcess File content to be processed
     * @return Email message
     */
    private StringBuilder processInputFiles(StringArrayFileContent fileToProcess) {
        StringBuilder emailMessage = new StringBuilder(fileToProcess.getFileName() + ":\r\n");

        // read individual files line by line
        List<String[]> fileContents = fileToProcess.getContents();
        ProcessCounts processCounts = new ProcessCounts();

        for (String[] lineContentArray : fileContents) {
            int lineLength = lineContentArray.length;

            if (lineLength == 2 || lineLength == 3) {
                // check to see if this is a header line
                // if it's a header row, we want to ignore it and move on
                String[] expandEnrollmentsHeader = CsvService.EXPAND_ENROLLMENT_HEADER.split(",");
                String[] expandEnrollmentsWithEmailHeader = CsvService.EXPAND_ENROLLMENT_HEADER_WITH_EMAIL.split(",");

                if (!Arrays.equals(lineContentArray, expandEnrollmentsHeader) && !Arrays.equals(lineContentArray, expandEnrollmentsWithEmailHeader)) {

                    String userId = lineContentArray[0];
                    String listingId = lineContentArray[1];

                    // see if there is a value for send_email
                    String sendEmailString = "false";
                    if (lineLength == 3) {
                        sendEmailString = lineContentArray[2];
                    }

                    // if the string is anything but "true", this will be set to false, which we want as a default
                    boolean sendEmail = Boolean.parseBoolean(sendEmailString);

                    processEnrollment(userId, listingId, sendEmail, emailMessage, processCounts);
                }
            }
        }

        emailMessage.append("\tUsers failed to be added to expand's enrollment: " + processCounts.getFailureCount() + "\r\n");
        emailMessage.append("\tUsers successfully added to expand's enrollment: " + processCounts.getSuccessCount() + "\r\n");
        emailMessage.append("\tTotal records processed: " + processCounts.getTotalCount() + "\r\n");

        return emailMessage;
    }

    private void processEnrollment(String canvasUserId, String listingId, boolean sendEmail, StringBuilder emailMessage, ProcessCounts processCounts) {
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

            user = userService.getUserBySisId(canvasUserId);

            if (user == null) {
                log.debug("Not found by sis id. Trying login id");
                user = userService.getUserBySisLoginId(canvasUserId);
            }
        } else { // must be login id
            // get canvasId for user by login id
            log.debug("Looking up user " + canvasUserId + " by sisLoginId");

            user = userService.getUserBySisLoginId(canvasUserId);

            if (user == null) {
                log.debug("Not found by login id. Trying sis id");
                user = userService.getUserBySisId(canvasUserId);
            }
        }

        if (user != null) {
            log.debug("found user = " + user.getName() + ", canvas id = " + user.getId());

            if (catalogListingService.addUserToListing(user.getId(), listingId, sendEmail)) {
                processCounts.incrementSuccessCount();
            } else {
                processCounts.incrementFailureCount();

                emailMessage.append(String.format("\tAn error occurred while attempting to enroll user %s in Expand listing %s. ", canvasUserId, listingId) +
                        "Double check the listing to make sure all required fields contain data and try to run the job again. If the problem persists, contact lmsreq@iu.edu for assistance.\r\n\r\n");
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
