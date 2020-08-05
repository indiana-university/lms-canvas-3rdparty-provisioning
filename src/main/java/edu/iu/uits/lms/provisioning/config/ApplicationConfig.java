package edu.iu.uits.lms.provisioning.config;

import edu.iu.uits.lms.common.oauth.OAuthConfig;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.method.configuration.EnableGlobalMethodSecurity;
import org.springframework.security.oauth2.client.DefaultOAuth2ClientContext;
import org.springframework.security.oauth2.client.OAuth2RestTemplate;
import org.springframework.security.oauth2.client.token.AccessTokenRequest;
import org.springframework.security.oauth2.client.token.DefaultAccessTokenRequest;
import org.springframework.security.oauth2.client.token.grant.password.ResourceOwnerPasswordResourceDetails;
import org.springframework.security.oauth2.common.AuthenticationScheme;
import org.springframework.web.servlet.config.annotation.EnableWebMvc;
import org.springframework.web.servlet.config.annotation.ResourceHandlerRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

@Configuration
@EnableWebMvc
@EnableGlobalMethodSecurity(securedEnabled = true, prePostEnabled = true)
@EnableConfigurationProperties(OAuthConfig.class)
@Slf4j
public class ApplicationConfig implements WebMvcConfigurer {

   @Autowired
   private OAuthConfig oAuthConfig;

   public ApplicationConfig() {
      log.debug("ApplicationConfig()");
   }

   @Override
   // used to read in various directories to add resources for the templates to use
   public void addResourceHandlers(ResourceHandlerRegistry registry) {
      registry.addResourceHandler("/app/css/**").addResourceLocations("classpath:/static/css/");
      registry.addResourceHandler("/app/js/**").addResourceLocations("classpath:/static/js/");
      registry.addResourceHandler("/app/webjars/**").addResourceLocations("/webjars/").resourceChain(true);
      registry.addResourceHandler("/app/jsrivet/**").addResourceLocations("classpath:/META-INF/resources/jsrivet/").resourceChain(true);
   }

   @Bean(name = "uaaRestTemplate")
   public OAuth2RestTemplate uaaRestTemplate() {
      ResourceOwnerPasswordResourceDetails resourceDetails = new ResourceOwnerPasswordResourceDetails();
      resourceDetails.setClientId(oAuthConfig.getClientId());
      resourceDetails.setClientSecret(oAuthConfig.getClientSecret());
      resourceDetails.setUsername(oAuthConfig.getClientId());
      resourceDetails.setPassword(oAuthConfig.getClientPassword());
      resourceDetails.setAccessTokenUri(oAuthConfig.getAccessTokenUri());
      resourceDetails.setClientAuthenticationScheme(AuthenticationScheme.form);

      AccessTokenRequest atr = new DefaultAccessTokenRequest();
      DefaultOAuth2ClientContext clientContext = new DefaultOAuth2ClientContext(atr);

      OAuth2RestTemplate restTemplate = new OAuth2RestTemplate(resourceDetails, clientContext);
      return restTemplate;
   }
}
