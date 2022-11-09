package edu.iu.uits.lms.provisioning.service;

import edu.iu.uits.lms.canvas.model.Account;
import edu.iu.uits.lms.canvas.model.Course;
import edu.iu.uits.lms.canvas.model.User;
import edu.iu.uits.lms.canvas.services.AccountService;
import edu.iu.uits.lms.canvas.services.CourseService;
import edu.iu.uits.lms.canvas.services.UserService;
import edu.iu.uits.lms.iuonly.services.SudsServiceImpl;
import edu.iu.uits.lms.provisioning.model.ImsUser;
import edu.iu.uits.lms.provisioning.model.content.FileContent;
import edu.iu.uits.lms.provisioning.model.content.StringArrayFileContent;
import edu.iu.uits.lms.provisioning.repository.ImsUserRepository;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.validator.routines.EmailValidator;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * Code for provisioning users into a Canvas enrollment
 */
@Slf4j
@Service
public class EnrollmentProvisioning {

    public static final String DELETED = "deleted";
    public static final String ACTIVE = "active";

    @Autowired
    private UserService userService = null;

    @Autowired
    private GuestAccountService guestAccountService = null;

    @Autowired
    private CsvService csvService = null;

    @Autowired
    private ImsUserRepository imsUserRepository = null;

    @Autowired
    private AccountService accountService;

    @Autowired
    private CourseService courseService;

    @Autowired
    private SudsServiceImpl sudsService;

    /**
     * Pass in a path to a csv file and a department code and this validate the data and send enrollments to Canvas
     * @param fileToProcess
     */
    public List<ProvisioningResult> processEnrollments(Collection<FileContent> fileToProcess, boolean allowSis, List<String> authorizedAccounts, boolean overrideRestrictions) {
        List<ProvisioningResult> prs = new ArrayList<>();
        for (FileContent file : fileToProcess) {
            StringBuilder emailMessage = new StringBuilder();

            StringBuilder finalMessage = new StringBuilder(file.getFileName() + ":\r\n");

            // process input files and transform into what will be sent off
            List<String[]> outputData = processInputFiles((StringArrayFileContent)file, emailMessage, allowSis, authorizedAccounts, overrideRestrictions);

            // Create csv file to send to Canvas
            String[] enrollmentsHeader = CsvService.ENROLLMENTS_HEADER_SECTION_LIMIT.split(",");

            byte[] fileBytes = null;
            boolean fileException = false;
            try {
                fileBytes = csvService.writeCsvToBytes(outputData, enrollmentsHeader);
                finalMessage.append(emailMessage);
            } catch (IOException e) {
                log.error("Error generating csv", e);
                finalMessage.append("\tThere were errors when generating the CSV file to send to Canvas\r\n");
                fileException = true;
            }

            prs.add(new ProvisioningResult(finalMessage, new ProvisioningResult.FileObject(file.getFileName(), fileBytes), fileException));
        }
        return prs;
    }

    /**
     * @param fileToProcess
     * @param emailMessage
     * @return
     */
    private List<String[]> processInputFiles(StringArrayFileContent fileToProcess, StringBuilder emailMessage, boolean allowSisEnrollments, List<String> authorizedAccounts, boolean overrideRestrictions) {
        List<String[]> stringArray = new ArrayList<>();

        int rejected = 0;
        int success = 0;
        int rowCounter = 0;

        // read individual files line by line
        List <String[]> fileContents = fileToProcess.getContents();
        Map<String, Boolean> sisCourses = new HashMap<>();
        Map<String, Boolean> sisSections = new HashMap<>();
        Map<String, Boolean> accountChecksMap = new HashMap<>();
        Map<String, String> courseAndAccountTrackerMap = new HashMap<>();

        for (String[] lineContentArray : fileContents) {
            // increment the counter here and not several places
            rowCounter++;

            int lineLength = lineContentArray.length;
            if (lineLength == 5 || lineLength == 6) {
                // check to see if this is a header line
                // if it's a header row, we want to ignore it and move on
                String[] enrollmentsHeader = CsvService.ENROLLMENTS_HEADER.split(",");
                String[] enrollmentsHeaderSectionLimit = CsvService.ENROLLMENTS_HEADER_SECTION_LIMIT.split(",");
                if (Arrays.equals(lineContentArray,enrollmentsHeader) || Arrays.equals(lineContentArray,enrollmentsHeaderSectionLimit)) {
                    continue;
                }

                String courseId = lineContentArray[0];
                String emailOrUserId = lineContentArray[1];
                String role = lineContentArray[2];
                String sectionId = lineContentArray[3];
                String status = lineContentArray[4];

                // if overrideRestrictions OR allowSis is true, skip the SIS checks
                boolean doSisCheck = !overrideRestrictions && !allowSisEnrollments;

                // check existing maps to see if we've looked up this info previously and
                if (doSisCheck) {
                    if (sisCourses.containsKey(courseId)) {
                        if (sisCourses.get(courseId)) {
                            // confirmed course is 'true' in the map, so skip the sis course lookup later
                            doSisCheck = false;
                        } else {
                            // confirmed course is 'false', so skip this line since we know it's not ok to use
                            log.warn("Skipped " + emailOrUserId + " because it is in a SIS course and we already checked.");
                            rejected++;
                            emailMessage.append("\tLine " + rowCounter + ": Enrollment for " + emailOrUserId + " rejected. Not authorized for SIS changes.\r\n");
                            continue;
                        }
                    }

                    if (sisSections.containsKey(sectionId)) {
                        if (sisSections.get(sectionId)) {
                            // confirmed section is 'true' in the map, so skip the sis section lookup later
                            doSisCheck = false;
                        } else {
                            // confirmed section is 'false', so skip this line since we know it's not ok to use
                            log.warn("Skipped " + emailOrUserId + " because it is in a SIS section and we already checked.");
                            rejected++;
                            emailMessage.append("\tLine " + rowCounter + ": Enrollment for " + emailOrUserId + " rejected. Not authorized for SIS changes.\r\n");
                            continue;
                        }
                    }
                }

                // if user does not have overrideRestrictions, allowSis, or the course/sections have not been looked up yet, do the SIS checks
                if (doSisCheck) {
                    // look up for SIS stuff
                    if (sudsService.getSudsCourseBySiteId(courseId) != null) {
                        log.warn("Skipped " + emailOrUserId + " because it is in a SIS course and user did not have SIS permission.");
                        rejected++;
                        emailMessage.append("\tLine " + rowCounter + ": Enrollment for " + emailOrUserId + " rejected. Not authorized for SIS changes.\r\n");
                        sisCourses.put(courseId, false);
                        continue;
                    } else if (sudsService.getSudsArchiveCourseBySiteId(courseId) != null) {
                        log.warn("Skipped " + emailOrUserId + " because it is in an archived SIS course and user did not have SIS permission.");
                        rejected++;
                        emailMessage.append("\tLine " + rowCounter + ": Enrollment for " + emailOrUserId + " rejected. Not authorized for SIS changes.\r\n");
                        sisCourses.put(courseId, false);
                        continue;
                    }

                    // look up for SIS stuff
                    if (sudsService.getSudsCourseBySiteId(sectionId) != null) {
                        log.warn("Skipped " + emailOrUserId + " because it is in an archived SIS section and user did not have SIS permission.");
                        rejected++;
                        emailMessage.append("\tLine " + rowCounter + ": Enrollment for " + emailOrUserId + " rejected. Not authorized for SIS changes.\r\n");
                        sisSections.put(sectionId, false);
                        continue;
                    } else if (sudsService.getSudsArchiveCourseBySiteId(sectionId) != null) {
                        log.warn("Skipped " + emailOrUserId + " because it is in an archived SIS section and user did not have SIS permission.");
                        rejected++;
                        emailMessage.append("\tLine " + rowCounter + ": Enrollment for " + emailOrUserId + " rejected. Not authorized for SIS changes.\r\n");
                        sisSections.put(sectionId, false);
                        continue;
                    }
                }

                // if we made it here, it passed the SIS checks. Add it to the lists
                sisCourses.put(courseId, true);
                sisSections.put(sectionId, true);

                boolean isAccountAuthorized = false;
                boolean accountPreviouslyAuthorized = false;

                // check existing maps to see if we've looked up this info previously and deal with it as appropriate
                if (accountChecksMap.containsKey(courseId)) {
                    if (accountChecksMap.get(courseId)) {
                        // we've checked this course's account before and verified, so let's set accountPreviouslyAuthorized to true to bypass the accounts check
                        accountPreviouslyAuthorized = true;
                    } else {
                        // account for course 'false', so skip this line since we know it's not ok to use
                        String accountName = courseAndAccountTrackerMap.get(courseId);
                        log.debug("Skipped " + emailOrUserId + " because we know they're not authorized from a previous lookup.");
                        emailMessage.append("\tLine " + rowCounter + ": Enrollment for " + emailOrUserId + " rejected. Not authorized to work in " + accountName + " subaccount.\r\n");
                        rejected++;
                        continue;
                    }
                }

                // if we made it here, this is a first time account lookup
                if (overrideRestrictions || accountPreviouslyAuthorized) {
                    isAccountAuthorized = true;
                } else if (authorizedAccounts.contains("ALL")) {
                    isAccountAuthorized = true;
                } else {
                    Course course = courseService.getCourse("sis_course_id:" + courseId);
                    if (course == null) {
                        // no course exists, so assuming new entry and letting Canvas deal with it
                        isAccountAuthorized = true;
                        accountChecksMap.put(courseId, true);
                    } else {
                        Account courseAccount = accountService.getAccount(course.getAccountId());
                        if (authorizedAccounts.contains(courseAccount.getName())) {
                            isAccountAuthorized = true;
                            accountChecksMap.put(courseId, true);
                        } else {
                            // course exists, so do checks to see the user is allowed in the node
                            String accountId = course.getAccountId();
                            List<String> parentAccountNames = accountService.getParentAccounts(accountId)
                                    .stream().map(parentNames -> parentNames.getName()).collect(Collectors.toList());

                            for (String authorizedAccount : authorizedAccounts) {
                                if (parentAccountNames.contains(authorizedAccount)) {
                                    isAccountAuthorized = true;
                                    accountChecksMap.put(courseId, true);
                                    break;
                                }
                            }

                            // if we made it to here, this course does not have an authorized account. Add to map for check in future lines.
                            if (!isAccountAuthorized) {
                                accountChecksMap.put(courseId, false);
                                courseAndAccountTrackerMap.put(courseId, courseAccount.getName());
                            }
                        }
                    }
                }

                if (isAccountAuthorized) {
                    // make sure the first object is an email address
                    log.info("Check for a valid email address");
                    if (EmailValidator.getInstance().isValid(emailOrUserId)) {
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

                        String[] lineToRewrite = {courseId,emailOrUserId,role,sectionId,status,sectionLimit};
                        // add the object to our list of users to send to Canvas
                        stringArray.add(lineToRewrite);
                        success++;
                    } else {
                        // Not a valid email address, so treat it as a normal IU account or emplId
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

                        boolean isEmplId = emailOrUserId.matches("[0-9]+");
                        String emplId = "";

                        if (isEmplId) {
                            // Put the error checking on Canvas!
                            emplId = emailOrUserId;
                        } else {
                            // Look up the userId from the IMS feed to get the 10 digit id
                            ImsUser imsUser = imsUserRepository.findByLoginId(emailOrUserId);
                            if (imsUser != null) {
                                emplId = imsUser.getUserId();
                            }

                            // if there is no emplId for a username lookup in IMS, try Canvas
                            if (emplId == null || "".equals(emplId)) {
                                // assure emplId is empty to make a status check easier later
                                emplId = "";

                                // regular username, so try this
                                User user = userService.getUserBySisLoginId(emailOrUserId);
                                if (user != null && user.getSisUserId() != null) {
                                    emplId = user.getSisUserId();
                                }

                                if (emplId.isEmpty()) {
                                    log.warn("Could not find emplId for: '" + emailOrUserId + "'");
                                    rejected++;
                                    emailMessage.append("\tLine " + rowCounter + ": no emplId found for " + emailOrUserId + "\r\n");
                                    continue;
                                }
                            }
                        }

                        String[] lineToRewrite = {courseId,emplId,role,sectionId,status,sectionLimit};

                        // add the object to our list of users to send to Canvas
                        stringArray.add(lineToRewrite);
                        success++;
                    }
                } else {
                    String accountName = courseAndAccountTrackerMap.get(courseId);
                    log.debug("Skipped " + emailOrUserId + " because provisioning user is not authorized to provision to this account.");
                    emailMessage.append("\tLine " + rowCounter + ": Enrollment for " + emailOrUserId + " rejected. Not authorized to work in " + accountName + " subaccount.\r\n");
                    rejected++;
                    continue;
                }
            }
        }
        int total = rejected + success;
        emailMessage.append("\tEnrollments rejected: " + rejected + "\r\n");
        emailMessage.append("\tEnrollments sent to Canvas: " + success + "\r\n");
        emailMessage.append("\tTotal enrollments processed: " + total + "\r\n");

        return stringArray;
    }
}