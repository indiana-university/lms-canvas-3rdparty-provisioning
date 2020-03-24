package edu.iu.uits.lms.provisioning.service;

import Services.ams.GuestInfo;
import canvas.client.generated.api.UsersApi;
import canvas.client.generated.model.CanvasLogin;
import canvas.client.generated.model.User;
import edu.iu.uits.lms.provisioning.model.ImsUser;
import edu.iu.uits.lms.provisioning.model.content.FileContent;
import edu.iu.uits.lms.provisioning.model.content.StringArrayFileContent;
import edu.iu.uits.lms.provisioning.repository.ImsUserRepository;
import lombok.extern.log4j.Log4j;
import org.apache.commons.validator.routines.EmailValidator;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;

/**
 * Code for provisioning users into a Canvas enrollment
 */
@Log4j
@Service
public class EnrollmentProvisioning {

    public static final String DELETED = "deleted";
    public static final String ACTIVE = "active";

    @Autowired
    private UsersApi usersApi = null;

    @Autowired
    private AmsServiceImpl amsService = null;

    @Autowired
    private CsvService csvService = null;

    @Autowired
    private ImsUserRepository imsUserRepository = null;

    /**
     * Pass in a path to a csv file and a department code and this validate the data and send enrollments to Canvas
     * @param fileToProcess
     */
    public List<ProvisioningResult> processEnrollments(Collection<FileContent> fileToProcess) {
        List<ProvisioningResult> prs = new ArrayList<>();
        for (FileContent file : fileToProcess) {
            StringBuilder emailMessage = new StringBuilder();

            StringBuilder finalMessage = new StringBuilder(file.getFileName() + ":\r\n");

            // process input files and transform into what will be sent off
            List<String[]> outputData = processInputFiles((StringArrayFileContent)file, emailMessage);

            // Create csv file to send to Canvas
            String[] enrollmentsHeader = CsvService.ENROLLMENTS_HEADER_SECTION_LIMIT.split(",");

            InputStream inputStream = null;
            boolean fileException = false;
            try {
                inputStream = csvService.writeCsvToStream(outputData, enrollmentsHeader);
                finalMessage.append(emailMessage);
            } catch (IOException e) {
                log.error("Error generating csv", e);
                finalMessage.append("\tThere were errors when generating the CSV file to send to Canvas\r\n");
                fileException = true;
            }

            prs.add(new ProvisioningResult(emailMessage, new ProvisioningResult.FileObject(file.getFileName(), inputStream), fileException));
        }
        return prs;
    }

    /**
     * @param fileToProcess
     * @param emailMessage
     * @return
     */
    private List<String[]> processInputFiles(StringArrayFileContent fileToProcess, StringBuilder emailMessage) {
        List<String[]> stringArray = new ArrayList<>();

            int successCount = 0;
            int failureCount = 0;
            int totalCount = 0;

            // read individual files line by line
            List <String[]> fileContents = fileToProcess.getContents();

            for (String[] lineContentArray : fileContents) {
                int lineLength = lineContentArray.length;
                if (lineLength == 5 || lineLength == 6) {
                    // check to see if this is a header line
                    // if it's a header row, we want to ignore it and move on
                    String[] enrollmentsHeader = CsvService.ENROLLMENTS_HEADER.split(",");
                    String[] enrollmentsHeaderSectionLimit = CsvService.ENROLLMENTS_HEADER_SECTION_LIMIT.split(",");
                    if (Arrays.equals(lineContentArray,enrollmentsHeader) || Arrays.equals(lineContentArray,enrollmentsHeaderSectionLimit)) {
                        continue;
                    }
                    // make sure the first object is an email address
                    log.info("Check for a valid email address");
                    if (EmailValidator.getInstance().isValid(lineContentArray[1])) {
                        String courseId = lineContentArray[0];
                        String email = lineContentArray[1];
                        String role = lineContentArray[2];
                        String sectionId = lineContentArray[3];
                        String status = lineContentArray[4];
                        // default to true, unless they specified or is a teacher
                        String sectionLimit = "true";
                        if (lineContentArray.length==6 && !lineContentArray[5].isEmpty()) {
                            if (lineContentArray[5].equalsIgnoreCase("true") || lineContentArray[5].equalsIgnoreCase("false")) {
                                sectionLimit = lineContentArray[5].toLowerCase();
                            }
                        } else if ("teacher".equals(role)) {
                            // extremely unlikely you'd make a new, guest account a teacher, but here it is anyway!
                            sectionLimit = "false";
                        }

                        // check to see if this email is in use
                        try {
                            List<CanvasLogin> canvasLoginsList =  usersApi.getGuestUserLogins(email);

                            // if there's results, find the one with the sis_user_id in it to write in the csv
                            if (!canvasLoginsList.isEmpty()) {
                                String sisUserId = "";
                                for (CanvasLogin canvasLogin : canvasLoginsList) {
                                    if (canvasLogin.getSisUserId() != null) {
                                        sisUserId = canvasLogin.getSisUserId();
                                        break;
                                    }
                                }
                                if (!sisUserId.isEmpty()) {
                                    String[] lineToRewrite = {courseId,sisUserId,role,sectionId,status,sectionLimit};
                                    // add the object to our list of users to send to Canvas
                                    stringArray.add(lineToRewrite);
                                    successCount++;
                                } else {
                                    // see if this is an Add before attempting to lookup in AMS, since a delete and AMS
                                    // would be the same data that we already know doesn't exist in Canvas
                                    if (ACTIVE.equals(status)) {
                                        GuestInfo guestInfo = amsService.lookupGuestByEmail(email);
                                        // Account exists, so add to our list to send to Canvas. It's likely a new user
                                        if (guestInfo != null && guestInfo.getStringError().isEmpty()) {
                                            String sequenceNumber = guestInfo.getStringSequenceNumber();
                                            String[] lineToRewrite = {courseId,sequenceNumber,role,sectionId,status,sectionLimit};
                                            // add the object to our list of users to send to Canvas
                                            stringArray.add(lineToRewrite);
                                            successCount++;
                                        } else {
                                            log.warn("No guest account found for '" + email + "' and no sis_user_id found in Canvas");
                                            failureCount++;
                                            emailMessage.append("\tNo guest account found for '" + email + "' and no sis_user_id found in Canvas\r\n");
                                        }
                                    } else {
                                        log.warn("No sis_user_id found in Canvas for " + email);
                                        failureCount++;
                                        emailMessage.append("\tNo sis_user_id found in Canvas for '" + email + "'\r\n");
                                    }
                                }
                            } else {
                                if (ACTIVE.equals(status)) {
                                    GuestInfo guestInfo = amsService.lookupGuestByEmail(email);
                                    // Account exists, so add to our list to send to Canvas. It's likely a new user
                                    if (guestInfo != null && guestInfo.getStringError().isEmpty()) {
                                        String sequenceNumber = guestInfo.getStringSequenceNumber();
                                        String[] lineToRewrite = {courseId,sequenceNumber,role,sectionId,status,sectionLimit};

                                        // add the object to our list of users to send to Canvas
                                        stringArray.add(lineToRewrite);
                                        successCount++;
                                    } else {
                                        log.warn("No guest account found for '" + email + "' and no sis_user_id found in Canvas");
                                        failureCount++;
                                        emailMessage.append("\tNo guest account found for '" + email + "' and no sis_user_id found in Canvas\r\n");
                                    }
                                } else {
                                    log.warn("No sis_user_id found in Canvas for " + email);
                                    failureCount++;
                                    emailMessage.append("\tNo sis_user_id found in Canvas for '" + email + "'\r\n");
                                }
                            }
                        } catch (IllegalStateException e) {
                            log.error("GuestAccountSOAPService error", e);
                            failureCount++;
                            emailMessage.append("\tNo sis_user_id found in Canvas for '" + email + "' and could not contact AMS service - " + e.getMessage() + "\r\n");
                        }
                    } else {
                        // Not a valid email address, so treat it as a normal IU account or emplId
                        String courseId = lineContentArray[0];
                        String userId = lineContentArray[1];
                        String role = lineContentArray[2];
                        String sectionId = lineContentArray[3];
                        String status = lineContentArray[4];
                        // default to true, unless csv specifies or is a teacher
                        String sectionLimit = "true";
                        if (lineContentArray.length==6 && !lineContentArray[5].isEmpty()) {
                            if (lineContentArray[5].equalsIgnoreCase("true") || lineContentArray[5].equalsIgnoreCase("false")) {
                                sectionLimit = lineContentArray[5].toLowerCase();
                            }
                        } else if ("teacher".equals(role)) {
                            // teacher role and was not specified in the csv
                            sectionLimit = "false";
                        }

                        boolean isEmplId = userId.matches("[0-9]+");
                        String emplId = "";

                        if (isEmplId) {
                            // Put the error checking on Canvas!
                            emplId = userId;
                        } else {
                            // Look up the userId from the IMS feed to get the 10 digit id
                            ImsUser imsUser = imsUserRepository.findByLoginId(userId);
                            if (imsUser != null) {
                                emplId = imsUser.getUserId();
                            }

                            // if there is no emplId for a username lookup in IMS, try Canvas
                            if (emplId == null || "".equals(emplId)) {
                                // assure emplId is empty to make a status check easier later
                                emplId = "";

                                // regular username, so try this
                                User user = usersApi.getUserBySisLoginId(userId);
                                if (user != null && user.getSisUserId() != null) {
                                    emplId = user.getSisUserId();
                                }

                                if (emplId.isEmpty()) {
                                    log.warn("Could not find emplId for: '" + userId + "'");
                                    failureCount++;
                                    totalCount++;
                                    emailMessage.append("\t" + userId + " - no emplId found\r\n");
                                    continue;
                                }
                            }
                        }

                        String[] lineToRewrite = {courseId,emplId,role,sectionId,status,sectionLimit};

                        // add the object to our list of users to send to Canvas
                        stringArray.add(lineToRewrite);
                        successCount++;
                    }
                }
                totalCount++;
            }
            emailMessage.append("\tAccounts failed: " + failureCount + "\r\n");
            emailMessage.append("\tAccounts successfully added to canvas enrollment provisioning csv: " + successCount + "\r\n");
            emailMessage.append("\tTotal records processed: " + totalCount + "\r\n");

        return stringArray;
    }
}