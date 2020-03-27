package edu.iu.uits.lms.provisioning.model;

import com.fasterxml.jackson.annotation.JsonFormat;
import edu.iu.uits.lms.common.date.DateFormatUtil;
import lombok.Data;
import lombok.NoArgsConstructor;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.Id;
import javax.persistence.PrePersist;
import javax.persistence.PreUpdate;
import javax.persistence.SequenceGenerator;
import javax.persistence.Table;
import javax.persistence.UniqueConstraint;
import java.util.Date;

/**
 * Created by chmaurer on 9/11/19.
 */
@Entity
@Table(name = "DEPT_AUTH_MESSAGE_SENDER",
      uniqueConstraints = @UniqueConstraint(name = "UK_DEPT_AUTH_MESSAGE_SENDER", columnNames = {"group_code", "email"}))
@SequenceGenerator(name = "DEPT_AUTH_MESSAGE_SENDER_ID_SEQ", sequenceName = "DEPT_AUTH_MESSAGE_SENDER_ID_SEQ", allocationSize = 1)
@Data
@NoArgsConstructor
public class DeptAuthMessageSender {
    @Id
    @GeneratedValue(generator = "DEPT_AUTH_MESSAGE_SENDER_ID_SEQ")
    private Long id;

    @Column(name = "GROUP_CODE")
    private String groupCode;

    @Column
    private String email;

    @Column
    private String name;

    @JsonFormat(pattern = DateFormatUtil.JSON_DATE_FORMAT)
    @Column(name = "CREATED")
    private Date createdOn;

    @JsonFormat(pattern = DateFormatUtil.JSON_DATE_FORMAT)
    @Column(name = "MODIFIED")
    private Date modifiedOn;


    @PreUpdate
    @PrePersist
    public void updateTimeStamps() {
        modifiedOn = new Date();
        if (createdOn==null) {
            createdOn = new Date();
        }
    }

}
