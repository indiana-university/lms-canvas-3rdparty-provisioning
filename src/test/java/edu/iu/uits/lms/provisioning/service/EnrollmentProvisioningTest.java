package edu.iu.uits.lms.provisioning.service;

/*-
 * #%L
 * lms-lti-3rdpartyprovisioning
 * %%
 * Copyright (C) 2015 - 2024 Indiana University
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
import edu.iu.uits.lms.canvas.services.AccountService;
import edu.iu.uits.lms.canvas.services.CourseService;
import edu.iu.uits.lms.canvas.services.UserService;
import edu.iu.uits.lms.iuonly.services.SisServiceImpl;
import edu.iu.uits.lms.provisioning.model.content.FileContent;
import edu.iu.uits.lms.provisioning.model.content.StringArrayFileContent;
import edu.iu.uits.lms.provisioning.repository.ImsUserRepository;
import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;

import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.List;

@Slf4j
@SpringBootTest(classes = EnrollmentProvisioning.class)
public class EnrollmentProvisioningTest {
   @Autowired
   private EnrollmentProvisioning enrollmentProvisioning;

   @MockBean
   private UserService userService = null;

   @MockBean
   private GuestAccountService guestAccountService = null;

   @MockBean
   private CsvService csvService = null;

   @MockBean
   private ImsUserRepository imsUserRepository = null;

   @MockBean
   private AccountService accountService;

   @MockBean
   private CourseService courseService;

   @MockBean
   private SisServiceImpl sisService;

   @Test
   public void processInputFiles_testSuccessfullyProcessSisCourseWithAllowSisEnrollmentsAndAllowOverrides() throws Exception {
      final String courseId = "";
      final String sectionId = "section1";
      final String userId = "user1";
      final String role = "student";
      final String action = "delete";

      final boolean allowSisEnrollments = true;
      final boolean allowOverides = true;

      final List<String[]> fileContentsList = new ArrayList<>();
      fileContentsList.add(new String[]{"course_id,user_id,role,section_id,status"});
      fileContentsList.add(new String[]{courseId, userId, role, sectionId, action});
      final StringArrayFileContent stringArrayFileContent = new StringArrayFileContent("file1.txt", fileContentsList);

      Mockito.when(sisService.isLegitSisCourse(sectionId)).thenReturn(false);

      final Method processInputFileMethod = enrollmentProvisioning.getClass().getDeclaredMethod("processInputFiles",
              StringArrayFileContent.class, StringBuilder.class, boolean.class, List.class, boolean.class);
      processInputFileMethod.setAccessible(true);

      final String sisUserId = "sisuserid1";
      final User user = new User();
      user.setSisUserId(sisUserId);

      final String isSectionLimit = Boolean.toString(true);

      Mockito.when(userService.getUserBySisLoginId(userId)).thenReturn(user);
      Mockito.when(sisService.isLegitSisCourse(sectionId)).thenReturn(true);

      final StringBuilder emailMessage = new StringBuilder();

      final List<String[]> results = (List<String[]>)
              processInputFileMethod.invoke(enrollmentProvisioning, stringArrayFileContent, emailMessage,
                      allowSisEnrollments, new ArrayList<>(), allowOverides);

      Assertions.assertNotNull(results);
      Assertions.assertEquals(1, results.size());

      final String[] resultsLine = results.get(0);

      Assertions.assertNotNull(resultsLine);
      Assertions.assertEquals(6, resultsLine.length);
      Assertions.assertEquals(courseId, resultsLine[0]);
      Assertions.assertEquals(sisUserId, resultsLine[1]);
      Assertions.assertEquals(role, resultsLine[2]);
      Assertions.assertEquals(sectionId, resultsLine[3]);
      Assertions.assertEquals(action, resultsLine[4]);
      Assertions.assertEquals(isSectionLimit, resultsLine[5]);

      final String emailMessageString = emailMessage.toString();

      Assertions.assertNotNull(emailMessageString);
      Assertions.assertTrue(emailMessageString.contains("Enrollments rejected: 0"));
      Assertions.assertTrue(emailMessageString.contains("Enrollments sent to Canvas: 1"));
      Assertions.assertTrue(emailMessageString.contains("Total enrollments processed: 1"));
   }

   @Test
   public void processInputFiles_testSuccessfullyProcessSisCourseWithAllowSisEnrollmentsOnly() throws Exception {
      final String courseId = "";
      final String sectionId = "section1";
      final String userId = "user1";
      final String role = "student";
      final String action = "delete";

      final boolean allowSisEnrollments = true;
      final boolean allowOverides = false;

      final List<String[]> fileContentsList = new ArrayList<>();
      fileContentsList.add(new String[]{"course_id,user_id,role,section_id,status"});
      fileContentsList.add(new String[]{courseId, userId, role, sectionId, action});
      final StringArrayFileContent stringArrayFileContent = new StringArrayFileContent("file1.txt", fileContentsList);

      Mockito.when(sisService.isLegitSisCourse(sectionId)).thenReturn(false);

      final Method processInputFileMethod = enrollmentProvisioning.getClass().getDeclaredMethod("processInputFiles",
              StringArrayFileContent.class, StringBuilder.class, boolean.class, List.class, boolean.class);
      processInputFileMethod.setAccessible(true);

      final String sisUserId = "sisuserid1";
      final User user = new User();
      user.setSisUserId(sisUserId);

      final String isSectionLimit = Boolean.toString(true);

      Mockito.when(userService.getUserBySisLoginId(userId)).thenReturn(user);
      Mockito.when(sisService.isLegitSisCourse(sectionId)).thenReturn(true);

      final StringBuilder emailMessage = new StringBuilder();

      final List<String[]> results = (List<String[]>)
              processInputFileMethod.invoke(enrollmentProvisioning, stringArrayFileContent, emailMessage,
                      allowSisEnrollments, new ArrayList<>(), allowOverides);

      Assertions.assertNotNull(results);
      Assertions.assertEquals(1, results.size());

      final String[] resultsLine = results.get(0);

      Assertions.assertNotNull(resultsLine);
      Assertions.assertEquals(6, resultsLine.length);
      Assertions.assertEquals(courseId, resultsLine[0]);
      Assertions.assertEquals(sisUserId, resultsLine[1]);
      Assertions.assertEquals(role, resultsLine[2]);
      Assertions.assertEquals(sectionId, resultsLine[3]);
      Assertions.assertEquals(action, resultsLine[4]);
      Assertions.assertEquals(isSectionLimit, resultsLine[5]);

      final String emailMessageString = emailMessage.toString();

      Assertions.assertNotNull(emailMessageString);
      Assertions.assertTrue(emailMessageString.contains("Enrollments rejected: 0"));
      Assertions.assertTrue(emailMessageString.contains("Enrollments sent to Canvas: 1"));
      Assertions.assertTrue(emailMessageString.contains("Total enrollments processed: 1"));
   }

   @Test
   public void processInputFiles_testSuccessfullyProcessSisCourseWithAllowOverridesOnly() throws Exception {
      final String courseId = "";
      final String sectionId = "section1";
      final String userId = "user1";
      final String role = "student";
      final String action = "delete";

      final boolean allowSisEnrollments = false;
      final boolean allowOverides = true;

      final List<String[]> fileContentsList = new ArrayList<>();
      fileContentsList.add(new String[]{"course_id,user_id,role,section_id,status"});
      fileContentsList.add(new String[]{courseId, userId, role, sectionId, action});
      final StringArrayFileContent stringArrayFileContent = new StringArrayFileContent("file1.txt", fileContentsList);

      Mockito.when(sisService.isLegitSisCourse(sectionId)).thenReturn(false);

      final Method processInputFileMethod = enrollmentProvisioning.getClass().getDeclaredMethod("processInputFiles",
              StringArrayFileContent.class, StringBuilder.class, boolean.class, List.class, boolean.class);
      processInputFileMethod.setAccessible(true);

      final String sisUserId = "sisuserid1";
      final User user = new User();
      user.setSisUserId(sisUserId);

      final String isSectionLimit = Boolean.toString(true);

      Mockito.when(userService.getUserBySisLoginId(userId)).thenReturn(user);
      Mockito.when(sisService.isLegitSisCourse(sectionId)).thenReturn(true);

      final StringBuilder emailMessage = new StringBuilder();

      final List<String[]> results = (List<String[]>)
              processInputFileMethod.invoke(enrollmentProvisioning, stringArrayFileContent, emailMessage,
                      allowSisEnrollments, new ArrayList<>(), allowOverides);

      Assertions.assertNotNull(results);
      Assertions.assertEquals(1, results.size());

      final String[] resultsLine = results.get(0);

      Assertions.assertNotNull(resultsLine);
      Assertions.assertEquals(6, resultsLine.length);
      Assertions.assertEquals(courseId, resultsLine[0]);
      Assertions.assertEquals(sisUserId, resultsLine[1]);
      Assertions.assertEquals(role, resultsLine[2]);
      Assertions.assertEquals(sectionId, resultsLine[3]);
      Assertions.assertEquals(action, resultsLine[4]);
      Assertions.assertEquals(isSectionLimit, resultsLine[5]);

      final String emailMessageString = emailMessage.toString();

      Assertions.assertNotNull(emailMessageString);
      Assertions.assertTrue(emailMessageString.contains("Enrollments rejected: 0"));
      Assertions.assertTrue(emailMessageString.contains("Enrollments sent to Canvas: 1"));
      Assertions.assertTrue(emailMessageString.contains("Total enrollments processed: 1"));
   }

   @Test
   public void processInputFiles_testFailureProcessSisCourseWithNoAllows() throws Exception {
      final String courseId = "";
      final String sectionId = "section1";
      final String userId = "user1";
      final String role = "student";
      final String action = "delete";

      final boolean allowSisEnrollments = false;
      final boolean allowOverides = false;

      final List<String[]> fileContentsList = new ArrayList<>();
      fileContentsList.add(new String[]{"course_id,user_id,role,section_id,status"});
      fileContentsList.add(new String[]{courseId, userId, role, sectionId, action});
      final StringArrayFileContent stringArrayFileContent = new StringArrayFileContent("file1.txt", fileContentsList);

      Mockito.when(sisService.isLegitSisCourse(sectionId)).thenReturn(false);

      final Method processInputFileMethod = enrollmentProvisioning.getClass().getDeclaredMethod("processInputFiles",
              StringArrayFileContent.class, StringBuilder.class, boolean.class, List.class, boolean.class);
      processInputFileMethod.setAccessible(true);

      final String sisUserId = "sisuserid1";
      final User user = new User();
      user.setSisUserId(sisUserId);

      Mockito.when(userService.getUserBySisLoginId(userId)).thenReturn(user);
      Mockito.when(sisService.isLegitSisCourse(sectionId)).thenReturn(true);

      final StringBuilder emailMessage = new StringBuilder();

      final List<String[]> results = (List<String[]>)
              processInputFileMethod.invoke(enrollmentProvisioning, stringArrayFileContent, emailMessage,
                      allowSisEnrollments, new ArrayList<>(), allowOverides);

      Assertions.assertNotNull(results);
      Assertions.assertEquals(0, results.size());

      final String emailMessageString = emailMessage.toString();

      Assertions.assertNotNull(emailMessageString);
      Assertions.assertTrue(emailMessageString.contains("Line 2: Enrollment for user1 rejected. Not authorized for SIS changes."));
      Assertions.assertTrue(emailMessageString.contains("Enrollments rejected: 1"));
      Assertions.assertTrue(emailMessageString.contains("Enrollments sent to Canvas: 0"));
      Assertions.assertTrue(emailMessageString.contains("Total enrollments processed: 1"));
   }
}