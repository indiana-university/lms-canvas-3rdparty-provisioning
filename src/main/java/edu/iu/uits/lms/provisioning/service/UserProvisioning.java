package edu.iu.uits.lms.provisioning.service;

import Services.ams.Guest;
import Services.ams.GuestInfo;
import Services.ams.GuestResponse;
import canvas.client.generated.api.CanvasApi;
import canvas.client.generated.api.UsersApi;
import canvas.client.generated.model.CanvasLogin;
import canvas.client.generated.model.User;
import canvas.helpers.UserHelper;
import edu.iu.uits.lms.provisioning.model.DeptAuthMessageSender;
import edu.iu.uits.lms.provisioning.model.content.CsvFileContent;
import edu.iu.uits.lms.provisioning.model.content.FileContent;
import email.client.generated.api.EmailApi;
import email.client.generated.model.EmailDetails;
import lombok.extern.log4j.Log4j;
import org.apache.commons.validator.routines.EmailValidator;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClientException;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;

/**
 * Code for provisioning users into Canvas
 */
@Log4j
@Service
public class UserProvisioning {
    private static final int EMAIL = 1;
    private static final int FIRST_NAME = 2;
    private static final int LAST_NAME = 3;

    @Autowired
    protected CustomNotificationService customNotificationService;

    @Autowired
    private EmailApi emailApi = null;

    @Autowired
    private CanvasApi canvasApi = null;

    @Autowired
    private UsersApi usersApi = null;

    @Autowired
    private AmsServiceImpl amsService = null;

    @Autowired
    private PasswordGeneratorImpl passwordGenerator = null;

    /**
     * Pass in a path to a csv file and a department code and this validate the data, attempt to make IU Guest Accounts,
     * create new Canvas accounts, and update existing Canvas accounts
     * @param fileToProcess
     */
    public StringBuilder processUsers(Collection<FileContent> fileToProcess, CustomNotificationBuilder customNotificationBuilder,
                                      String dept) {
        StringBuilder emailMessage = new StringBuilder();

//        List<File> fileList = new ArrayList<File>();
//        fileList.add(fileToProcess);

        DeptAuthMessageSender messageSender = customNotificationService.getValidatedCustomMessageSender(customNotificationBuilder, dept);

        // Create guest accounts
        List<User> canvasAccounts = createGuestAccounts(fileToProcess, emailMessage, messageSender, customNotificationBuilder);

        List<String[]> results = createUsersAndLogins(canvasAccounts);

        emailMessage = addCanvasResultsToEmail(emailMessage, results);

        return emailMessage;
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

        for (FileContent file : fileList) {
            int successCount = 0;
            int updateCount = 0;
            int failureCount = 0;
            int totalCount = 0;
            int emailSuccessCount = 0;
            List<String> emailFailures = new ArrayList<>();

            emailMessage.append(file.getFileName() + ":\r\n");

            // read individual files line by line
            List <String[]> fileContents = ((CsvFileContent)file).getContents();

            for (String[] lineContentArray : fileContents) {
                int lineLength = lineContentArray.length;
                if (lineLength == 6) {
                    // check to see if this is a header line
                    // if it's a header row, we want to ignore it and move on
                    String[] usersHeader = CsvService.USERS_HEADER_NO_SHORT_NAME.split(",");
                    if (Arrays.equals(lineContentArray,usersHeader)) {
                        continue;
                    }
                    // make sure the first object is an email address
                    log.info("Check for a valid email address");
                    if (EmailValidator.getInstance().isValid(lineContentArray[EMAIL])) {
                        Guest ga = new Guest();
                        ga.setStringEmail(lineContentArray[EMAIL]);
                        ga.setStringFirstName(lineContentArray[FIRST_NAME]);
                        ga.setStringLastName(lineContentArray[LAST_NAME]);

                        // check to see if this email is in use
                        try {
                            GuestInfo guestInfo = amsService.lookupGuestByEmail(ga.getStringEmail());
                            if (guestInfo != null && "".equals(guestInfo.getStringError())) {
                                // Account already exists, but add to our list to send to Canvas
                                User cu = new User();

                                // Even though we have info provided from the csv file, let's use what's officially registered with AMS
                                String sequenceNumber = guestInfo.getStringSequenceNumber();
                                String email = guestInfo.getStringEmail();
                                String firstName = guestInfo.getStringFirstName();
                                String lastName = guestInfo.getStringLastName();

                                // The userId in this case is the "sequence id" from the guest account
                                cu.setSisUserId(sequenceNumber);
                                cu.setLoginId(email);
                                UserHelper.setAllNameFields(cu, firstName, lastName);
                                cu.setEmail(email);

                                // add the object to our list of users to send to Canvas
                                canvasGuestAccountList.add(cu);
                                updateCount++;
                            } else {
                                ga.setStringPassword(passwordGenerator.generatePassword(14));
                                GuestResponse gr = amsService.createGuest(ga);
                                User cu = new User();
                                // The userId in this case is the "sequence id" from the guest account
                                cu.setSisUserId(gr.getStringSequenceNumber());
                                cu.setLoginId(ga.getStringEmail());
                                UserHelper.setAllNameFields(cu, ga.getStringFirstName(), ga.getStringLastName());
                                cu.setEmail(ga.getStringEmail());
                                canvasGuestAccountList.add(cu);
                                successCount++;
                                // Since we have the information for the new account, send the activation email to the newly created user
                                sendActivationEmail(cu, gr.getStringActivationCode());
                                if (messageSender != null) {
                                    customNotificationService.sendCustomWelcomeMessage(ga.getStringEmail(), customNotificationBuilder);
                                    emailSuccessCount++;
                                } else if (customNotificationBuilder.isFileExists() && (!customNotificationBuilder.isFileValid() || messageSender == null)) {
                                    emailFailures.add(ga.getStringEmail());
                                }
                            }
                        } catch (IllegalStateException e) {
                            log.error("GuestAccountSOAPService error", e);
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

    private void sendActivationEmail(User cu, String activationCode) {
        String body = "An Indiana University Guest Account has been created for you with this email address as your username. The account was created on your behalf at the request of an IU school or department so you can participate in an educational offering in Canvas. You must activate the account and set your password before you can access Canvas and other IU services.\r\n\r\n";

        body += "To activate your account go to https://ams.iu.edu/guests/ActivateGuest.aspx and enter the username and confirmation code below.  You must activate the account within 10 days.\r\n\r\n";

        body += "Your IU Guest username is: " + cu.getLoginId() + "\r\n";
        body += "Your confirmation code is: " + activationCode + "\r\n\r\n";

        body += "To set your password, go to https://ams.iu.edu/guests/ForgotGuestPassword.aspx, enter your email address and submit the form.  You will receive another email message with instructions and a confirmation code for resetting your password.  Choose a new password that is easy for you to remember, but difficult for others to guess.\r\n\r\n";

        body += "Once you have activated your account and set your password, you can log in to Canvas at: https://canvas.iu.edu.\r\n\r\n";

        body += "If you do not need or did not request this account, no further action is needed. The account will remain inactive and expire automatically in 30 days. If you have any questions about this account please send e-mail to ithelp@indiana.edu.";

        String subject = "Activate Your Indiana University Guest Account ASAP";

        try {
            EmailDetails details = new EmailDetails();
            details.addRecipientsItem(cu.getEmail());
            details.setSubject(subject);
            details.setBody(body);
            emailApi.sendEmail(details);
        } catch (RestClientException e) {
            // since this isn't using an attachment this exception should never be thrown
            log.error("Error sending email", e);
        }
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
            intendedNewLogin.setUniqueId(canvasUser.getSisUserId());

            List<CanvasLogin> logins = null;
            try {
                logins = usersApi.getGuestUserLogins(canvasUser.getEmail());
//                logins = userService.getGuestUserLogins(canvasUser.getEmail());
            } catch (Exception e) {
                failedUsers.add(new String[]{canvasUser.getEmail(), e.getMessage()});
                continue;
            }
            String userId = null;

            if (logins == null || logins.isEmpty()) {
                try {
                    userId = usersApi.createUserWithUser(canvasAccountId, canvasUser);
//                    userId = userService.createUser(canvasAccountId, canvasUser);
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
                        usersApi.createLogin(canvasAccountId, userId, canvasUser.getSisUserId(), null, canvasUser.getSisUserId());
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
}