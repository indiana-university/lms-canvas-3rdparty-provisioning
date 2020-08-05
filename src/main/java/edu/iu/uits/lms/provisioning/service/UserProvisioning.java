package edu.iu.uits.lms.provisioning.service;

import canvas.client.generated.api.CanvasApi;
import canvas.client.generated.api.UsersApi;
import canvas.client.generated.model.CanvasLogin;
import canvas.client.generated.model.User;
import canvas.helpers.UserHelper;
import edu.iu.uits.lms.provisioning.config.ToolConfig;
import edu.iu.uits.lms.provisioning.model.DeptAuthMessageSender;
import edu.iu.uits.lms.provisioning.model.GuestAccount;
import edu.iu.uits.lms.provisioning.model.content.FileContent;
import edu.iu.uits.lms.provisioning.model.content.StringArrayFileContent;
import email.client.generated.api.EmailApi;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.validator.routines.EmailValidator;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

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

    @Autowired
    protected CustomNotificationService customNotificationService;

    @Autowired
    private EmailApi emailApi = null;

    @Autowired
    private CanvasApi canvasApi = null;

    @Autowired
    private UsersApi usersApi = null;

    @Autowired
    private GuestAccountService guestAccountService = null;

    @Autowired
    private ToolConfig toolConfig = null;

    /**
     * Pass in a path to a csv file and a department code and this validate the data, attempt to make IU Guest Accounts,
     * create new Canvas accounts, and update existing Canvas accounts
     * @param filesToProcess
     */
    public ProvisioningResult processUsers(Collection<FileContent> filesToProcess, CustomNotificationBuilder customNotificationBuilder,
                                      String dept) {
        StringBuilder emailMessage = new StringBuilder();

        DeptAuthMessageSender messageSender = customNotificationService.getValidatedCustomMessageSender(customNotificationBuilder, dept);

        // Create guest accounts
        List<User> canvasAccounts = createGuestAccounts(filesToProcess, emailMessage, messageSender, customNotificationBuilder);

        List<String[]> results = createUsersAndLogins(canvasAccounts);

        emailMessage = addCanvasResultsToEmail(emailMessage, results);
        return new ProvisioningResult(emailMessage, null, false);
    }

    /**
     * Tack on the results of the canvas account creations to the email
     *
     * @param emailMessage
     * @param results
     */
    private StringBuilder addCanvasResultsToEmail(StringBuilder emailMessage, List<String[]> results) {
        if (results.size() > 0) {
            emailMessage.append("\tCanvas accounts failed: " + results.size() + "\r\n");
            for (String[] result : results) {
                String email = result[0];
                String status = result[1];
                emailMessage.append("\t\t" + email + " - " + status + "\r\n");
            }
        }
        else {
            emailMessage.append("\tAll user records(except noted above) were successfully sent to Canvas for this file!\r\n");
        }

        return emailMessage;
    }

    private List<User> createGuestAccounts(Collection<FileContent> fileList, StringBuilder emailMessage,
                                                  DeptAuthMessageSender messageSender, CustomNotificationBuilder customNotificationBuilder) {
        // Build the legit list to pass on to Canvas
        List<User> canvasGuestAccountList = new ArrayList<User>();

        String[] usersHeader = CsvService.USERS_HEADER_NO_SHORT_NAME.split(",");
        String[] usersHeaderWithService = CsvService.USERS_HEADER_NO_SHORT_NAME_ADD_SERVICE.split(",");

        for (FileContent file : fileList) {
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
                                User cu = new User();

                                // Even though we have info provided from the csv file, let's use what's officially registered with AMS
                                String sequenceNumber = guestInfo.getExternalAccountId();
                                String email = guestInfo.getRegistrationEmail();
                                String firstName = guestInfo.getFirstName();
                                String lastName = guestInfo.getLastName();

                                // sis_user_id = email, login/unique login = cirrusId
                                cu.setSisUserId(email);
                                cu.setLoginId(sequenceNumber);
                                UserHelper.setAllNameFields(cu, firstName, lastName);
                                cu.setEmail(email);

                                // add the object to our list of users to send to Canvas
                                canvasGuestAccountList.add(cu);
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

                                    User cu = new User();
                                    // sis_user_id = email, login/unique login = cirrusId
                                    cu.setSisUserId(ga.getRegistrationEmail());
                                    cu.setLoginId(gr.getExternalAccountId());
                                    UserHelper.setAllNameFields(cu, ga.getFirstName(), ga.getLastName());
                                    cu.setEmail(ga.getRegistrationEmail());
                                    canvasGuestAccountList.add(cu);
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
        }
        return canvasGuestAccountList;
    }

    /**
     * Create users and logins in canvas, using the input list
     *
     * @param canvasUsers Input list of users to create in canvas
     * @return List of failed users.  It's a string array with the first value being the email address and the second value being the failure message
     */
    private List<String[]> createUsersAndLogins(List<User> canvasUsers) {
        List<String[]> failedUsers = new ArrayList<String[]>();

        String canvasAccountId = canvasApi.getRootAccount();

        for (User canvasUser : canvasUsers) {

            CanvasLogin intendedNewLogin = new CanvasLogin();
            intendedNewLogin.setAccountId(canvasAccountId);
            // the login_id/cirrusId will be Canvas's unique_id
            intendedNewLogin.setUniqueId(canvasUser.getLoginId());

            List<CanvasLogin> logins = null;
            try {
                logins = usersApi.getGuestUserLogins(canvasUser.getEmail());
            } catch (Exception e) {
                failedUsers.add(new String[]{canvasUser.getEmail(), e.getMessage()});
                continue;
            }
            String userId = null;

            if (logins == null || logins.isEmpty()) {
                try {
                    userId = usersApi.createUserWithUser(canvasAccountId, canvasUser);
                } catch (Exception e) {
                    failedUsers.add(new String[]{canvasUser.getEmail(), e.getMessage()});
                }
            } else {
                //Just pick the first one in the list
                userId = logins.get(0).getUserId();
                log.debug("Canvas user already exists.  Using '" + userId + "'");
            }
            if (userId != null) {
                intendedNewLogin.setUserId(userId);
                if (logins == null || !logins.contains(intendedNewLogin)) {
                    try {
                        usersApi.createLogin(canvasAccountId, userId, canvasUser.getSisUserId(), canvasUser.getSisUserId(), null);
                    } catch (Exception e) {
                        failedUsers.add(new String[]{canvasUser.getEmail(), e.getMessage()});
                    }
                } else {
                    log.debug("Login '" + canvasUser.getSisUserId() + "' already exists for user '" + userId + "'");
                }
            }
        }
        return failedUsers;
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