package edu.iu.uits.lms.provisioning.service.exception;

import java.util.Collections;
import java.util.List;

public class FileUploadException extends ProvisioningException {
   private static final String ERR_TITLE = "Something went wrong while uploading your files to Canvas.";
   private static final String ERR_DESC = "No files were sent due to a server error.  Please try again.  If the problem persists, refer to the KB Doc above for details on contacting the LMS team for support.";

   public FileUploadException(String message) {
      super(message);
   }

   public List<FileError> getFileErrors() {
      return Collections.singletonList(new FileError(ERR_TITLE, ERR_DESC));
   }
}
