package edu.iu.uits.lms.provisioning.model;

import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.Id;
import javax.persistence.SequenceGenerator;
import javax.persistence.Table;
import javax.persistence.UniqueConstraint;

//import edu.iu.uits.lms.services.DateFormatUtil;

/**
 * Created by chmaurer on 9/11/19.
 */
@EqualsAndHashCode(callSuper = true)
@Entity
@Table(name = "DEPT_AUTH_MESSAGE_SENDER",
      uniqueConstraints = @UniqueConstraint(name = "UK_DEPT_AUTH_MESSAGE_SENDER", columnNames = {"group_code", "email"}))
@SequenceGenerator(name = "DEPT_AUTH_MESSAGE_SENDER_ID_SEQ", sequenceName = "DEPT_AUTH_MESSAGE_SENDER_ID_SEQ", allocationSize = 1)
@Data
@NoArgsConstructor
public class DeptAuthMessageSender extends ModelWithDates {
    @Id
    @GeneratedValue(generator = "DEPT_AUTH_MESSAGE_SENDER_ID_SEQ")
    private Long id;

    @Column(name = "GROUP_CODE")
    private String groupCode;

    @Column
    private String email;

    @Column
    private String name;

}
