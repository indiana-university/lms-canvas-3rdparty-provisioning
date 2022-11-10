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

import edu.iu.uits.lms.canvas.services.AccountService;
import edu.iu.uits.lms.iuonly.services.SudsServiceImpl;
import edu.iu.uits.lms.provisioning.model.content.FileContent;
import edu.iu.uits.lms.provisioning.model.content.StringArrayFileContent;
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
public class CourseProvisioning {

    @Autowired
    private AccountService accountService;

    @Autowired
    private CsvService csvService = null;

    @Autowired
    private SudsServiceImpl sudsService;

    /**
     * Pass in a path to a csv file and a department code and this validate the data and send enrollments to Canvas
     * It is also assumed that if you're calling this method, you've passed in a valid Canvas course.csv header!
     * @param fileToProcess
     */
    public List<ProvisioningResult> processCourses(Collection<FileContent> fileToProcess, List<String> authorizedAccounts, boolean overrideRestrictions) {
        List<ProvisioningResult> prs = new ArrayList<>();
        StringBuilder emailMessage = new StringBuilder();
        StringBuilder errorMessage = new StringBuilder();
        List<String[]> stringArrayList = new ArrayList<>();
        Map<String, Boolean> sisCourses = new HashMap<>();
        Map<String, Boolean> accountChecksMap = new HashMap<>();
        int rejected = 0;
        int success = 0;

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
                    errorMessage.append("\tLine " + rowCounter + ": Row did not match the amount of fields specified in the header. Skipping. Double check the amount of commas and try again.\r\n");
                    rejected++;
                    continue;
                }

                // courseId is always the first entry in the courses.csv
                String courseId = lineContentArray[0];

                // if the user has override permissions, do not worry about the other lookups and move on!
                if (overrideRestrictions) {
                    // Everything is cool, add it in!
                    log.debug("Successfully added in row " + rowCounter);
                    stringArrayList.add(lineContentArray);
                    success++;
                    continue;
                }

                boolean doSisCheck = true;

                if (sisCourses.containsKey(courseId)) {
                    if (sisCourses.get(courseId)) {
                        // confirmed course is 'true' in the map, so skip the suds lookup later
                        doSisCheck = false;
                    } else {
                        // confirmed course is 'false', so skip this line since we know it's not ok to use
                        log.debug("Skipped " + rowCounter + " because it is a SIS course and we already checked.");
                        errorMessage.append("\tLine " + rowCounter + ": Course " + courseId + " rejected. Not authorized for SIS changes.\r\n");
                        rejected++;
                        continue;
                    }
                }

                // look up for SIS stuff
                if (doSisCheck) {
                    if (sudsService.getSudsCourseBySiteId(courseId) != null) {
                        log.debug("Skipped " + rowCounter + " because it is a SIS course and user did not have SIS permission.");
                        errorMessage.append("\tLine " + rowCounter + ": Course " + courseId + " rejected. Not authorized for SIS changes.\r\n");
                        sisCourses.put(courseId, false);
                        rejected++;
                        continue;
                    } else if (sudsService.getSudsArchiveCourseBySiteId(courseId) != null) {
                        log.debug("Skipped " + rowCounter + " because it is an archived SIS course and user did not have SIS permission.");
                        errorMessage.append("\tLine " + rowCounter + ": Course " + courseId + " rejected. Not authorized for SIS changes.\r\n");
                        sisCourses.put(courseId, false);
                        rejected++;
                        continue;
                    }
                }

                // made it here, so it passed the SIS checks
                sisCourses.put(courseId, true);

                boolean isAccountAuthorized = false;
                // accountId is the 4th item in the row
                String account = lineContentArray[3];

                // check existing maps to see if we've looked up this info previously and deal with it as appropriate
                if (accountChecksMap.containsKey(account)) {
                    if (accountChecksMap.get(account)) {
                        // we've checked this course's account before and verified it is cool, so let's set isAccountAuthorized to true to bypass the accounts check
                        log.debug("Already verified " + account + " check because we already verified it as good.");
                        isAccountAuthorized = true;
                    } else {
                        // account for course 'false', so skip this line since we know it's not ok to use
                        log.debug("Skipped " + rowCounter + " because user is not authorized to provision to this account and was previously checked.");
                        errorMessage.append("\tLine " + rowCounter + ": Course " + courseId + " rejected. Not authorized to work in " + account + " subaccount.\r\n");
                        rejected++;
                        continue;
                    }
                }

                // this check will happen, unless an account from a previous line has been checked
                if (!isAccountAuthorized) {
                    if (authorizedAccounts != null) {
                        if (authorizedAccounts.contains("ALL")) {
                            // have authority for all nodes, so set the boolean
                            isAccountAuthorized = true;
                        } else if (authorizedAccounts.contains(account)) {
                            // account in the csv is the same as authorized, so let's assume they're cool and shenanigans is not involved
                            isAccountAuthorized = true;
                        } else {
                            // get the parent account names
                            List<String> parentAccountNames = accountService.getParentAccounts("sis_account_id:"+account)
                                    .stream().map(parentNames -> parentNames.getName()).collect(Collectors.toList());

                            for (String authorizedAccount : authorizedAccounts) {
                                if (parentAccountNames.contains(authorizedAccount)) {
                                    isAccountAuthorized = true;
                                    accountChecksMap.put(account, true);
                                    break;
                                }
                            }

                            // if we made it to here, this course does not have an authorized account. Add to map for check in future lines.
                            if (!isAccountAuthorized) {
                                accountChecksMap.put(account, false);
                            }
                        }
                    }
                }

                if (isAccountAuthorized) {
                    // Everything is cool, add it in!
                    log.debug("Successfully added in row " + rowCounter);
                    stringArrayList.add(lineContentArray);
                    success++;
                    continue;
                } else {
                    log.debug("Skipped " + rowCounter + " because user is not authorized to provision to this account.");
                    errorMessage.append("\tLine " + rowCounter + ": Course " + courseId + " rejected. Not authorized to work in " + account + " subaccount.\r\n");
                    rejected++;
                    continue;
                }
            }

            StringBuilder finalMessage = new StringBuilder(emailMessage);
            byte[] fileBytes = null;
            boolean fileException = false;
            try {
                // Create csv file to send to Canvas
                fileBytes = csvService.writeCsvToBytes(stringArrayList, null);
                int total = rejected + success;

                if (errorMessage.length() > 0) {
                    finalMessage.append(errorMessage);
                    finalMessage.append("\tCourses rejected: " + rejected + "\r\n");
                    finalMessage.append("\tCourses sent to Canvas: " + success + "\r\n");
                    finalMessage.append("\tTotal courses processed: " + total + "\r\n");
                } else {
                    finalMessage.append("\tCourses rejected: " + rejected + "\r\n");
                    finalMessage.append("\tCourses sent to Canvas: " + success + "\r\n");
                    finalMessage.append("\tTotal courses processed: " + total + "\r\n");
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
