package edu.iu.uits.lms.provisioning.config;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.PropertySource;

@Configuration
@ConfigurationProperties(prefix = "ams.service")
@PropertySource(value = {"classpath:env.properties",
      "classpath:default.properties",
      "classpath:application.properties",
      "${app.fullFilePath}/lms.properties",
      "${app.fullFilePath}/protected.properties",
      "${app.fullFilePath}/security.properties"}, ignoreResourceNotFound = true)
@Getter
@Setter
public class AmsConfig {
   private String url;
   private String user;
   private String password;
}
