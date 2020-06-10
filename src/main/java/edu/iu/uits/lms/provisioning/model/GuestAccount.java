package edu.iu.uits.lms.provisioning.model;

import lombok.Data;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;

@Data
public class GuestAccount {
   private String externalAccountId;
   private String accountStatus;
   private String firstName;
   private String lastName;
   private String registrationEmail;
   private Date created;
   private List<LinkedAccount> linkedAccounts;
   private List<String> errorMessages = new ArrayList<>();
   private String serviceName;

   @Data
   private static class LinkedAccount {
      private String linkedAccountId;
      private String accountType;
      private String accountStatus;
      private String internetAddress;
      private Date expirationDate;
      private Date created;
   }
}
