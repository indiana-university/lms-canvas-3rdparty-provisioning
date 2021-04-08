package edu.iu.uits.lms.provisioning.model;

import lombok.Data;

import java.io.Serializable;

@Data
public class NotificationForm implements Serializable {
   private String sender;
   private String subject;
   private String body;
}
