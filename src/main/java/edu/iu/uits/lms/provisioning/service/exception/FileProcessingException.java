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
