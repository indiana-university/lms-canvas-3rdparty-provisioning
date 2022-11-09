package edu.iu.uits.lms.provisioning.config;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.client.BufferingClientHttpRequestFactory;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.web.client.RestTemplate;

@Configuration
@Slf4j
public class ExpandEnvironmentConfig {

   @Autowired
   ExpandConfiguration expandConfiguration;

   @Bean(name = "ExpandRestTemplate")
   public RestTemplate restTemplate() {
      RestTemplate restTemplate = new RestTemplate(new BufferingClientHttpRequestFactory(new SimpleClientHttpRequestFactory()));

      restTemplate.getInterceptors().add(new ExpandTokenAuthorizationInterceptor(expandConfiguration.getToken()));
//        restTemplate.getInterceptors().add(new LoggingRequestInterceptor());

      return restTemplate;
   }
}
