package edu.iu.uits.lms.provisioning.service;

import lombok.AllArgsConstructor;
import lombok.Data;

import java.text.MessageFormat;
import java.util.ArrayList;
import java.util.List;

public class FileParsingException extends Exception {
   private static final String ERR_TITLE_PATTERN = "The format of {0} did not conform to any of the documented formats.";
   private static final String ERR_DESC_PATTERN = "No files will be processed. Correct {0} and try again.";

   private List<String> fileNames;

   public FileParsingException(String message, List<String> fileNames) {
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

   @Data
   @AllArgsConstructor
   public static class FileError {
      private String title;
      private String description;
   }
}
