package edu.iu.uits.lms.provisioning.config;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

@Configuration
@ConfigurationProperties(prefix = "expand")
@Getter
@Setter
public class ExpandConfiguration {

   private String token;
   private String baseUrl;
   private String baseApiUrl;
}
