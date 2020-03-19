package edu.iu.uits.lms.provisioning.model;

import lombok.Data;
import lombok.EqualsAndHashCode;

import javax.persistence.CollectionTable;
import javax.persistence.Column;
import javax.persistence.ElementCollection;
import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.Id;
import javax.persistence.JoinColumn;
import javax.persistence.NamedQueries;
import javax.persistence.NamedQuery;
import javax.persistence.SequenceGenerator;
import javax.persistence.Table;
import java.util.List;

@EqualsAndHashCode(callSuper = true)
@Entity
@Table(name = "DEPT_PROV_USERS")
@NamedQueries({
//        @NamedQuery(name = "User.findByUsername", query = "from User where username = :username"),
        @NamedQuery(name = "User.findByCanvasUserId", query = "from User where canvas_user_id = :canvasUserId"),
        @NamedQuery(name = "User.findAllAuthorizedSenders", query = "from User where authorizedSender = true order by username asc"),
        @NamedQuery(name = "User.findAllAuthorizedUsers", query = "from User where authorizedUser = true order by username asc")
})
//@DiscriminatorColumn(name = "USER_TYPE")
//@Inheritance(strategy = InheritanceType.SINGLE_TABLE)
@SequenceGenerator(name = "DEPT_PROV_USERS_ID_SEQ", sequenceName = "DEPT_PROV_USERS_ID_SEQ", allocationSize = 1)
@Data
//@RequiredArgsConstructor
public class User extends ModelWithDates {

   @Id
   @GeneratedValue(generator = "DEPT_PROV_USERS_ID_SEQ")
   @Column(name = "DEPT_PROV_USERS_ID")
   private Long id;

   @Column(name = "DISPLAY_NAME")
   private String displayName;

   @Column(name = "USERNAME")
   private String username;

   @Column(name = "CANVAS_USER_ID")
   private String canvasUserId;

   @Column(name = "GROUP_CODE")
   @ElementCollection
   @CollectionTable(name = "DEPT_PROV_USER_GROUP", joinColumns = @JoinColumn(name = "DEPT_PROV_USERS_ID"))
   private List<String> groupCode;

}
