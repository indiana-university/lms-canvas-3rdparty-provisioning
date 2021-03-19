package edu.iu.uits.lms.provisioning.config;

import edu.iu.uits.lms.provisioning.model.NotificationForm;
import edu.iu.uits.lms.provisioning.model.content.FileContent;
import edu.iu.uits.lms.provisioning.service.DeptRouter;
import lombok.AllArgsConstructor;
import lombok.Data;
import org.apache.commons.collections4.MultiValuedMap;

import java.io.Serializable;

@Data
@AllArgsConstructor
public class BackgroundMessage implements Serializable {
   private MultiValuedMap<DeptRouter.CSV_TYPES, FileContent> filesByType;
   private String department;
   private NotificationForm notificationForm;
   private Long archiveId;
   private String username;

}
