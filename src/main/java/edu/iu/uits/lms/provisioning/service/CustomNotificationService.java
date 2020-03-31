package edu.iu.uits.lms.provisioning.service;

import edu.iu.uits.lms.provisioning.model.DeptAuthMessageSender;
import edu.iu.uits.lms.provisioning.repository.DeptAuthMessageSenderRepository;
import email.client.generated.api.EmailApi;
import email.client.generated.model.EmailDetails;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

@Service
public class CustomNotificationService {

   @Autowired
   private EmailApi emailApi = null;

   @Autowired
   private DeptAuthMessageSenderRepository deptAuthMessageSenderRepository = null;

   public void sendCustomWelcomeMessage(String email, CustomNotificationBuilder customNotificationBuilder) {
      EmailDetails details = new EmailDetails();
      details.addRecipientsItem(email);
      details.setSubject(customNotificationBuilder.getSubject());
      details.setBody(customNotificationBuilder.getBody());
      details.setEnableHtml(false);
      details.setPriority(EmailDetails.PriorityEnum.NORMAL);
      details.setFrom(customNotificationBuilder.getSender());
      emailApi.sendEmail(details, false);
   }

   public DeptAuthMessageSender getValidatedCustomMessageSender(CustomNotificationBuilder customNotificationBuilder, String dept) {
      DeptAuthMessageSender authSender = null;
      if (customNotificationBuilder.isFileExists() && customNotificationBuilder.isFileValid()) {
         authSender = deptAuthMessageSenderRepository.findByGroupCodeIgnoreCaseAndEmailIgnoreCase(dept, customNotificationBuilder.getSender());
      }

      return authSender;
   }
}
