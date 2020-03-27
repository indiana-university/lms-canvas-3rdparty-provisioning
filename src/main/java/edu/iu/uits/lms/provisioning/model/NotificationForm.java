package edu.iu.uits.lms.provisioning.model;

import lombok.Data;

@Data
public class NotificationForm {
   private String sender;
   private String subject;
   private String body;
}
