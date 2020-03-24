package edu.iu.uits.lms.provisioning.service;

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

   String START_DATE = ",start_date";
   String END_DATE = ",end_date";

   String EXPAND_ENROLLMENT_HEADER = "user_id,listing_id";


   void writeCsv(List<String[]> stringArrayList, String[] headerArray, String filePath) throws IOException;
   InputStream writeCsvToStream(List<String[]> stringArrayList, String[] headerArray) throws IOException;

   File zipCsv(List<ProvisioningResult.FileObject> fileList, String filePath);

   List<File> filterFiles(File folder);

}
