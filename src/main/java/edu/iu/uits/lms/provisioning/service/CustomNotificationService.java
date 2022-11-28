package edu.iu.uits.lms.provisioning.service;

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

import edu.iu.uits.lms.email.model.EmailDetails;
import edu.iu.uits.lms.email.model.Priority;
import edu.iu.uits.lms.email.service.EmailService;
import edu.iu.uits.lms.email.service.LmsEmailTooBigException;
import edu.iu.uits.lms.provisioning.model.DeptAuthMessageSender;
import edu.iu.uits.lms.provisioning.repository.DeptAuthMessageSenderRepository;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import javax.mail.MessagingException;

@Service
@Slf4j
public class CustomNotificationService {

   @Autowired
   private EmailService emailService = null;

   @Autowired
   private DeptAuthMessageSenderRepository deptAuthMessageSenderRepository = null;

   public void sendCustomWelcomeMessage(String email, CustomNotificationBuilder customNotificationBuilder) {
      EmailDetails details = new EmailDetails();
      details.setRecipients(new String[] {email});
      details.setSubject(customNotificationBuilder.getSubject());
      details.setBody(customNotificationBuilder.getBody());
      details.setEnableHtml(false);
      details.setPriority(Priority.NORMAL);
      details.setFrom(customNotificationBuilder.getSender());
      try {
         emailService.sendEmail(details, false);
      } catch (LmsEmailTooBigException | MessagingException e) {
         log.error("Unable to send email", e);
      }
   }

   public DeptAuthMessageSender getValidatedCustomMessageSender(CustomNotificationBuilder customNotificationBuilder, String dept) {
      DeptAuthMessageSender authSender = null;
      if (customNotificationBuilder.isFileExists() && customNotificationBuilder.isFileValid()) {
         authSender = deptAuthMessageSenderRepository.findByGroupCodeIgnoreCaseAndEmailIgnoreCase(dept, customNotificationBuilder.getSender());
      }

      return authSender;
   }
}
