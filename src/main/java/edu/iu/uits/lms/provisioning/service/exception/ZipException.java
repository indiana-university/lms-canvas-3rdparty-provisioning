package edu.iu.uits.lms.provisioning.service.exception;

import java.text.MessageFormat;
import java.util.Collections;
import java.util.List;

public class ZipException extends ProvisioningException {
   private static final String ERR_TITLE_PATTERN = "Something went wrong while zipping/archiving your {0} files.";
   private static final String ERR_DESC_BLOCKED = "No files were sent due to a server error.  Please try again.  If the problem persists, refer to the KB Doc above for details on contacting the LMS team for support.";
   private static final String ERR_DESC = "All files were sent to Canvas successfully, there were some problems in the final stages with the local archiving process.";
   public static final String CANVAS_ZIP = "Canvas";
   public static final String ORIGINALS_ZIP = "Uploaded";

   private String whichZip;
   private boolean blocking;

   public ZipException(String whichZip, boolean blocking, String message) {
      super(message);
      this.whichZip = whichZip;
      this.blocking = blocking;
   }

   public List<FileError> getFileErrors() {
      String description = blocking ? ERR_DESC_BLOCKED : ERR_DESC;
      return Collections.singletonList(new FileError(MessageFormat.format(ERR_TITLE_PATTERN, whichZip), description));
   }
}
