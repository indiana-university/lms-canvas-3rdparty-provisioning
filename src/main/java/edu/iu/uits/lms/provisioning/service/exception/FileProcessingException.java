package edu.iu.uits.lms.provisioning.service.exception;

import java.text.MessageFormat;
import java.util.ArrayList;
import java.util.List;

public class FileProcessingException extends ProvisioningException {
   private static final String ERR_TITLE_PATTERN = "Something went wrong while processing {0}, even though the file meets basic formatting specifications.";
   private static final String ERR_DESC_PATTERN = "No files will be sent to Canvas. Double check that {0} is correct and try again.  If the problem persists, refer to the KB Doc above for details on contacting the LMS team for support.";

   private List<String> fileNames;

   public FileProcessingException(String message, List<String> fileNames) {
      super(message);
      this.fileNames = fileNames;
   }

   public List<FileError> getFileErrors() {
      List<FileError> fileErrors = new ArrayList<>();
      for (String fileName : fileNames) {
         fileErrors.add(new FileError(MessageFormat.format(ERR_TITLE_PATTERN, fileName),
               MessageFormat.format(ERR_DESC_PATTERN, fileName)));
      }
      return fileErrors;
   }
}
