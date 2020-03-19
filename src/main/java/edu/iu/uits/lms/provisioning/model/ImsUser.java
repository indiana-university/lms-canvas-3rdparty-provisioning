package edu.iu.uits.lms.provisioning.model;

import lombok.Getter;
import lombok.NoArgsConstructor;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.Id;
import javax.persistence.Table;

@Entity
@Table(name = "IMS.IMS_CANVAS_USERS_30_V")
@Getter
@NoArgsConstructor
public class ImsUser {

   @Id
   @Column(name = "user_id")
   private String userId;

   @Column(name = "login_id")
   private String loginId;

   @Column(name = "first_name")
   private String firstName;

   @Column(name = "last_name")
   private String lastName;

   private String email;
}
