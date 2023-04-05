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

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.util.List;

public interface CsvService {

   /**
    * COURSES_HEADER can also be used with CsvService.START_DATE and/or CsvService.END_DATE, but keep START_DATE
    * before END_DATE if you use both!
    * Example: CsvService.COURSES_HEADER + CsvService.START_DATE + CsvService.END_DATE
    */
   String COURSES_HEADER = "course_id,short_name,long_name,account_id,term_id,status";

   /**
    * COURSES_HEADER_NO_TERM is the same as COURSES_HEADER, but does not include term_id
    * COURSES_HEADER_NO_TERM can also be used with CsvService.START_DATE and/or CsvService.END_DATE, but keep START_DATE
    * before END_DATE if you use both!
    * Example: CsvService.COURSES_HEADER_NO_TERM + CsvService.START_DATE + CsvService.END_DATE
    */
   String COURSES_HEADER_NO_TERM = "course_id,short_name,long_name,account_id,status";
   String ENROLLMENTS_HEADER = "course_id,user_id,role,section_id,status";
   String ENROLLMENTS_HEADER_SECTION_LIMIT = "course_id,user_id,role,section_id,status,limit_section_privileges";

   /**
    * SECTIONS_HEADER can also be used with CsvService.START_DATE and/or CsvService.END_DATE, but keep START_DATE
    * before END_DATE if you use both!
    * Example: CsvService.SECTIONS_HEADER + CsvService.START_DATE + CsvService.END_DATE
    */
   String SECTIONS_HEADER = "section_id,course_id,name,status";

   String USERS_HEADER = "user_id,login_id,first_name,last_name,short_name,email,status";

   /**
    * USERS_HEADER_NO_SHORT_NAME is the same as USERS_HEADER, but does not contain the short_name field
    */
   String USERS_HEADER_NO_SHORT_NAME = "user_id,login_id,first_name,last_name,email,status";
   String USERS_HEADER_NO_SHORT_NAME_ADD_SERVICE = "user_id,login_id,first_name,last_name,email,status,service";


   String START_DATE = ",start_date";
   String END_DATE = ",end_date";

   String EXPAND_ENROLLMENT_HEADER = "user_id,listing_id";
   String EXPAND_ENROLLMENT_HEADER_WITH_EMAIL = "user_id,listing_id,send_email";

   String BLUEPRINT_COURSE_ID = ",blueprint_course_id";


   void writeCsv(List<String[]> stringArrayList, String[] headerArray, String filePath) throws IOException;
   InputStream writeCsvToStream(List<String[]> stringArrayList, String[] headerArray) throws IOException;
   byte[] writeCsvToBytes(List<String[]> stringArrayList, String[] headerArray) throws IOException;

   File zipCsv(List<ProvisioningResult.FileObject> fileList, String filePath);

   List<File> filterFiles(File folder);

}
