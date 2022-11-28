package edu.iu.uits.lms.provisioning.service.exception;

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
