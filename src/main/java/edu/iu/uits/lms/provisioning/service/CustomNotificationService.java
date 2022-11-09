package edu.iu.uits.lms.provisioning.service;

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
