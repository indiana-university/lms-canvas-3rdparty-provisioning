package edu.iu.uits.lms.provisioning.service;

import com.opencsv.CSVReader;
import com.opencsv.exceptions.CsvException;
import edu.iu.uits.lms.provisioning.model.content.ByteArrayFileContent;
import edu.iu.uits.lms.provisioning.model.content.FileContent;
import edu.iu.uits.lms.provisioning.model.content.StringArrayFileContent;
import edu.iu.uits.lms.provisioning.service.exception.FileParsingException;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.collections4.MultiValuedMap;
import org.apache.commons.collections4.multimap.ArrayListValuedHashMap;
import org.apache.commons.lang3.StringUtils;
import org.springframework.web.multipart.MultipartFile;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.List;

@Slf4j
public class FileParsingUtil {

   private static final String PROP_FILE = "message.properties";

   public static MultiValuedMap<DeptRouter.CSV_TYPES, FileContent> parseFiles(MultipartFile[] files, boolean customUsersNotification) throws FileParsingException {
      MultiValuedMap<DeptRouter.CSV_TYPES, FileContent> filesByType = new ArrayListValuedHashMap<>();
      List<String> errors = new ArrayList<>();
      boolean foundUsersFile = false;

      for (MultipartFile file : files) {
         try {
            byte[] fileBytes = file.getBytes();
            List<String[]> fileContents = new CSVReader(new InputStreamReader(new ByteArrayInputStream(fileBytes))).readAll();
            FileContent fc = new StringArrayFileContent(file.getOriginalFilename(), fileContents);
            ByteArrayFileContent bafc = new ByteArrayFileContent(file.getOriginalFilename(), fileBytes);
            filesByType.put(DeptRouter.CSV_TYPES.ORIGINALS, bafc);

            //Get the first line, hopefully headers!
            String[] firstLine = fileContents.get(0);
            switch (StringUtils.join(firstLine, ",")) {
               case CsvService.COURSES_HEADER:
               case CsvService.COURSES_HEADER + CsvService.START_DATE:
               case CsvService.COURSES_HEADER + CsvService.END_DATE:
               case CsvService.COURSES_HEADER + CsvService.START_DATE + CsvService.END_DATE:
               case CsvService.COURSES_HEADER_NO_TERM:
               case CsvService.COURSES_HEADER_NO_TERM + CsvService.START_DATE:
               case CsvService.COURSES_HEADER_NO_TERM + CsvService.END_DATE:
               case CsvService.COURSES_HEADER_NO_TERM + CsvService.START_DATE + CsvService.END_DATE:
                  filesByType.put(DeptRouter.CSV_TYPES.COURSES, fc);
                  break;
               case CsvService.ENROLLMENTS_HEADER:
               case CsvService.ENROLLMENTS_HEADER_SECTION_LIMIT:
                  filesByType.put(DeptRouter.CSV_TYPES.ENROLLMENTS, fc);
                  break;
               case CsvService.SECTIONS_HEADER:
               case CsvService.SECTIONS_HEADER + CsvService.START_DATE:
               case CsvService.SECTIONS_HEADER + CsvService.END_DATE:
               case CsvService.SECTIONS_HEADER + CsvService.START_DATE + CsvService.END_DATE:
                  FileContent fcOverride = new ByteArrayFileContent(file.getOriginalFilename(), fileBytes);
                  filesByType.put(DeptRouter.CSV_TYPES.SECTIONS, fcOverride);
                  break;
               case CsvService.USERS_HEADER_NO_SHORT_NAME:
               case CsvService.USERS_HEADER_NO_SHORT_NAME_ADD_SERVICE:
                  filesByType.put(DeptRouter.CSV_TYPES.USERS, fc);
                  foundUsersFile = true;
                  break;
               case CsvService.EXPAND_ENROLLMENT_HEADER:
                  filesByType.put(DeptRouter.CSV_TYPES.EXPAND_ENROLLMENTS, fc);
                  break;
               default:
                  if (PROP_FILE.equalsIgnoreCase(fc.getFileName())) {
                     filesByType.put(DeptRouter.CSV_TYPES.PROPERTIES, bafc);
                  } else {
                     filesByType.put(DeptRouter.CSV_TYPES.BAD, fc);
                     errors.add(fc.getFileName());
                  }
                  break;
            }
         } catch (IOException | CsvException | IndexOutOfBoundsException e) {
            log.error("Error processing " + file.getOriginalFilename(), e);
            errors.add(file.getOriginalFilename());
         }
      }
      boolean wantedButMissing = customUsersNotification && !foundUsersFile;
      if (!errors.isEmpty() || wantedButMissing) {
         throw new FileParsingException("Error parsing some uploaded files", errors, wantedButMissing);
      }
      return filesByType;
   }
}
