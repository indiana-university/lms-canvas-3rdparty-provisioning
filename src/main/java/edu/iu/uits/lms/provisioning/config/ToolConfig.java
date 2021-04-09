package edu.iu.uits.lms.provisioning.config;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

@Configuration
@ConfigurationProperties(prefix = "deptprov")
@Getter
@Setter
public class ToolConfig {
   private String version;
   private String env;
   private String guestAccountCreationUrl;
   private String canvasServiceName;
   private String expandServiceName;
   private String backgroundQueueName;
   private String defaultBatchNotificationEmail;
}
