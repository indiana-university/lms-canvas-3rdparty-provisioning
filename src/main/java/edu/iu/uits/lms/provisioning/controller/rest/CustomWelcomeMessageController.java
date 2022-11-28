package edu.iu.uits.lms.provisioning.controller.rest;

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

import edu.iu.uits.lms.provisioning.model.DeptAuthMessageSender;
import edu.iu.uits.lms.provisioning.service.CustomNotificationBuilder;
import edu.iu.uits.lms.provisioning.service.CustomNotificationService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;

@RestController
@RequestMapping("/rest/dept_message_tester")
@Tag(name = "CustomWelcomeMessageController", description = "Validate details for a custom welcome message and send a test email message")
public class CustomWelcomeMessageController {

   @Autowired
   private CustomNotificationService customNotificationService = null;

   @PostMapping(value="/validate")
   @Operation(summary = "Validate the given properties and send a test email welcome message")
   public ResponseEntity validatePropertiesFile(@RequestParam("messagePropertiesFile") MultipartFile messagePropertiesFile,
                                                @RequestParam("department") String department) throws IOException {

      CustomNotificationBuilder cnb = new CustomNotificationBuilder(messagePropertiesFile.getInputStream());
      if (!cnb.isFileValid()) {
         return ResponseEntity.status(HttpStatus.BAD_REQUEST).body("properties file has missing or invalid values");
      }

      DeptAuthMessageSender validatedCustomMessageSender = customNotificationService.getValidatedCustomMessageSender(cnb, department);

      if (validatedCustomMessageSender != null) {
         customNotificationService.sendCustomWelcomeMessage(validatedCustomMessageSender.getEmail(), cnb);
         return ResponseEntity.ok("test email sent");
      }
      return ResponseEntity.status(HttpStatus.BAD_REQUEST).body("email specified in file is not authorized to send messages for the '" + department + "' department");
   }

}
