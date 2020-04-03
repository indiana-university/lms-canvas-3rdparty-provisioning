package edu.iu.uits.lms.provisioning.service.exception;

import java.text.MessageFormat;
import java.util.ArrayList;
import java.util.List;

public class FileParsingException extends ProvisioningException {
   private static final String ERR_TITLE_PATTERN = "The format of {0} did not conform to any of the documented formats.";
   private static final String ERR_DESC_PATTERN = "No files will be processed. Correct {0} and try again.";

   private List<String> fileNames;
   boolean missingUsersFile;

   public FileParsingException(String message, List<String> fileNames, boolean missingUsersFile) {
      super(message);
      this.fileNames = fileNames;
      this.missingUsersFile = missingUsersFile;
   }

   public List<FileError> getFileErrors() {
      List<FileError> fileErrors = new ArrayList<>();
      for (String fileName : fileNames) {
         fileErrors.add(new FileError(MessageFormat.format(ERR_TITLE_PATTERN, fileName),
               MessageFormat.format(ERR_DESC_PATTERN, fileName)));
      }
      if (missingUsersFile) {
         fileErrors.add(new FileError("No files found that match the 'users' format",
               "You indicated that you wanted to send custom notifications to new guest users, but did not provide a users file.  Either provide a properly formatted file, or refrain from checking the notification option."));
      }
      return fileErrors;
   }

}
