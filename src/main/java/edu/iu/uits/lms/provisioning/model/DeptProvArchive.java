package edu.iu.uits.lms.provisioning.model;

import com.fasterxml.jackson.annotation.JsonFormat;
import edu.iu.uits.lms.common.date.DateFormatUtil;
import lombok.Data;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.Id;
import javax.persistence.PrePersist;
import javax.persistence.PreUpdate;
import javax.persistence.SequenceGenerator;
import javax.persistence.Table;
import java.util.Date;

@Entity
@Table(name = "DEPT_PROV_ARCHIVES")
@SequenceGenerator(name = "DEPT_PROV_ARCHIVES_ID_SEQ", sequenceName = "DEPT_PROV_ARCHIVES_ID_SEQ", allocationSize = 1)
@Data
public class DeptProvArchive {

   @Id
   @GeneratedValue(generator = "DEPT_PROV_ARCHIVES_ID_SEQ")
   private Long id;

   @Column(name = "GROUP_CODE")
   private String department;

   @Column(name = "ORIG_DISPLAY_NAME")
   private String originalDisplayName;

   @Column(name = "ORIGINAL_CONTENT")
   private byte[] originalContent;

   @Column(name = "CANVAS_DISPLAY_NAME")
   private String canvasDisplayName;

   @Column(name = "CANVAS_CONTENT")
   private byte[] canvasContent;

   @Column(name = "CANVAS_IMPORT_ID")
   private String canvasImportId;

   @Column(name = "USERNAME")
   private String username;

   @JsonFormat(pattern= DateFormatUtil.JSON_DATE_FORMAT)
   @Column(name = "CREATED_ON")
   private Date createdOn;
   @JsonFormat(pattern= DateFormatUtil.JSON_DATE_FORMAT)
   @Column(name = "MODIFIED_ON")
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
