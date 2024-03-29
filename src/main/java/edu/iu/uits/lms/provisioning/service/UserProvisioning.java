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

import edu.iu.uits.lms.provisioning.config.ToolConfig;
import edu.iu.uits.lms.provisioning.model.DeptAuthMessageSender;
import edu.iu.uits.lms.provisioning.model.GuestAccount;
import edu.iu.uits.lms.provisioning.model.content.FileContent;
import edu.iu.uits.lms.provisioning.model.content.StringArrayFileContent;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.validator.routines.EmailValidator;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;

/**
 * Code for provisioning users into Canvas
 */
@Slf4j
@Service
public class UserProvisioning {
    private static final int EMAIL = 1;
    private static final int FIRST_NAME = 2;
    private static final int LAST_NAME = 3;
    private static final int SERVICE = 6;
    private static final String SERVICE_VALUE_C = "c";
    private static final String SERVICE_VALUE_E = "e";
    private static final String ACTIVE_VALUE = "active";

    @Autowired
    protected CustomNotificationService customNotificationService;

    @Autowired
    private GuestAccountService guestAccountService = null;

    @Autowired
    private ToolConfig toolConfig = null;

    @Autowired
    private CsvService csvService = null;

    /**
     * Pass in a path to a csv file and a department code and this validate the data, attempt to make IU Guest Accounts,
     * create new Canvas accounts, and update existing Canvas accounts
     * @param filesToProcess Files to process
     * @param customNotificationBuilder CustomNotificationBuilder
     * @param dept Department to be processed
     * @return List of ProvisioningResult objects
     */
    public List<ProvisioningResult> processUsers(Collection<FileContent> filesToProcess, CustomNotificationBuilder customNotificationBuilder,
                                      String dept) {
        List<ProvisioningResult> prs = new ArrayList<>();
        StringBuilder emailMessage = new StringBuilder();

        DeptAuthMessageSender messageSender = customNotificationService.getValidatedCustomMessageSender(customNotificationBuilder, dept);

        for (FileContent file : filesToProcess) {
            // Create guest accounts
            List<String[]> canvasAccounts = createGuestAccounts(file, emailMessage, messageSender, customNotificationBuilder);

            // Create csv file to send to Canvas
            String[] usersHeader = CsvService.USERS_HEADER_NO_SHORT_NAME.split(",");

            byte[] fileBytes = null;
            boolean fileException = false;
            try {
                fileBytes = csvService.writeCsvToBytes(canvasAccounts, usersHeader);
            } catch (IOException e) {
                log.error("Error generating csv", e);
                emailMessage.append("\tThere were errors when generating the CSV file to send to Canvas\r\n");
                fileException = true;
            }

            prs.add(new ProvisioningResult(emailMessage, new ProvisioningResult.FileObject(file.getFileName(), fileBytes), fileException));
        }
        return prs;
    }

    private List<String[]> createGuestAccounts(FileContent file, StringBuilder emailMessage,
                                                  DeptAuthMessageSender messageSender, CustomNotificationBuilder customNotificationBuilder) {
        // Build the legit list to pass on to Canvas
        List<String[]> stringArray = new ArrayList<>();

        String[] usersHeader = CsvService.USERS_HEADER_NO_SHORT_NAME.split(",");
        String[] usersHeaderWithService = CsvService.USERS_HEADER_NO_SHORT_NAME_ADD_SERVICE.split(",");

        int successCount = 0;
        int updateCount = 0;
        int failureCount = 0;
        int totalCount = 0;
        int emailSuccessCount = 0;
        List<String> emailFailures = new ArrayList<>();

        emailMessage.append(file.getFileName() + ":\r\n");

        // read individual files line by line
        List <String[]> fileContents = ((StringArrayFileContent)file).getContents();

        boolean usersCsvHasService = false;

        for (String[] lineContentArray : fileContents) {
            int lineLength = lineContentArray.length;
            if (lineLength == 6 || lineLength == 7) {
                // Assume sending to Canvas until proven otherwise
                String serviceValue = SERVICE_VALUE_C;
                // check to see if this is a header line
                // if it's a header row, we want to ignore it and move on
                if (Arrays.equals(lineContentArray,usersHeader)) {
                    continue;
                } else if (Arrays.equals(lineContentArray, usersHeaderWithService)) {
                    usersCsvHasService = true;
                    continue;
                }
                // make sure the first object is an email address
                log.info("Check for a valid email address");
                if (EmailValidator.getInstance().isValid(lineContentArray[EMAIL])) {
                    GuestAccount ga = new GuestAccount();
                    ga.setRegistrationEmail(lineContentArray[EMAIL]);
                    ga.setFirstName(lineContentArray[FIRST_NAME]);
                    ga.setLastName(lineContentArray[LAST_NAME]);
                    if (usersCsvHasService) {
                        if (SERVICE_VALUE_E.equalsIgnoreCase(lineContentArray[SERVICE])) {
                            serviceValue = SERVICE_VALUE_E;
                        }
                    }
                    ga.setServiceName(serviceNameHelper(serviceValue));

                    // check to see if this email is in use
                    try {
                        GuestAccount guestInfo = guestAccountService.lookupGuestByEmail(ga.getRegistrationEmail());
                        if (guestInfo != null) {
                            // Account already exists, but add to our list to send to Canvas
//                                "user_id,login_id,first_name,last_name,email,status";
                            String[] lineToRewrite = {guestInfo.getRegistrationEmail(), guestInfo.getExternalAccountId(), guestInfo.getFirstName(), guestInfo.getLastName(), guestInfo.getRegistrationEmail(), ACTIVE_VALUE};
                            stringArray.add(lineToRewrite);
                            updateCount++;
                        } else {
                            GuestAccount gr = guestAccountService.createGuest(ga);

                            if (gr==null) {
                                log.error("GuestAccountService error: returned null");
                                failureCount++;
                                emailMessage.append("\t" + lineContentArray[EMAIL] + " - GuestAccountService error: returned null\r\n");
                            } else if (!gr.getErrorMessages().isEmpty()) {
                                StringBuilder errorMessageBuilder = new StringBuilder();
                                for (String errorMessage : gr.getErrorMessages()) {
                                    errorMessageBuilder.append(errorMessage + "\r");
                                }
                                log.error("Guest Account creation error: " + errorMessageBuilder);
                                failureCount++;
                                emailMessage.append("\t" + lineContentArray[EMAIL] + " - Guest Account creation error: " + errorMessageBuilder + "\r\n");
                            } else {
                                // send custom message of a successful guest account before attempting anything else in the code
                                if (messageSender != null) {
                                    customNotificationService.sendCustomWelcomeMessage(ga.getRegistrationEmail(), customNotificationBuilder);
                                    emailSuccessCount++;
                                } else if (customNotificationBuilder.isFileExists() && (!customNotificationBuilder.isFileValid() || messageSender == null)) {
                                    emailFailures.add(ga.getRegistrationEmail());
                                }

//                                    "user_id,login_id,first_name,last_name,email,status";
                                String[] lineToRewrite = {ga.getRegistrationEmail(), gr.getExternalAccountId(), ga.getFirstName(), ga.getLastName(), ga.getRegistrationEmail(), ACTIVE_VALUE};
                                stringArray.add(lineToRewrite);
                                successCount++;
                            }
                        }
                    } catch (Exception e) {
                        log.error("GuestAccountService error", e);
                        failureCount++;
                        emailMessage.append("\t" + lineContentArray[EMAIL] + " - " + e.getMessage() + "\r\n");
                    }
                } else {
                    log.warn("Skipping invalid email address: '" + lineContentArray[EMAIL] + "'");
                    failureCount++;
                    emailMessage.append("\t" + lineContentArray[EMAIL] + " - invalid email address. Will not attempt to make a guest account.\r\n");
                }
            }
            totalCount++;
        }
        emailMessage.append("\tGuest accounts creation failures: " + failureCount + "\r\n");
        emailMessage.append("\tGuest accounts successfully created: " + successCount + "\r\n");
        emailMessage.append("\tGuest account updates sent to Canvas: " + updateCount + "\r\n");
        emailMessage.append("\tTotal records processed: " + totalCount + "\r\n");
        if (customNotificationBuilder.isFileExists()) {
            emailMessage.append("\tTotal custom email messages sent: " + emailSuccessCount + "\r\n");

            emailMessage.append("\tTotal custom email message failures: " + emailFailures.size() + "\r\n");
            for (String email : emailFailures) {
                emailMessage.append("\t\t- " + email + "\r\n");
            }
        }
        return stringArray;
    }

    private String serviceNameHelper(String serviceValue) {
        String fullServiceValueName = "";
        if (SERVICE_VALUE_C.equals(serviceValue)) {
            fullServiceValueName = toolConfig.getCanvasServiceName();
        } else if (SERVICE_VALUE_E.equals(serviceValue)) {
            fullServiceValueName = toolConfig.getExpandServiceName();
        }

        // just return an empty string if we don't have a proper service value, although the code prior to this forces
        // a default of SERVICE_VALUE_C
        return fullServiceValueName;
    }
}
