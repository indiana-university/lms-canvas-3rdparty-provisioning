package edu.iu.uits.lms.provisioning.config;

import edu.iu.uits.lms.common.oauth.OAuthConfig;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.core.Queue;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.method.configuration.EnableGlobalMethodSecurity;
import org.springframework.security.oauth2.client.OAuth2AuthorizationContext;
import org.springframework.security.oauth2.client.OAuth2AuthorizeRequest;
import org.springframework.security.oauth2.client.OAuth2AuthorizedClientManager;
import org.springframework.security.oauth2.client.OAuth2AuthorizedClientProvider;
import org.springframework.security.oauth2.client.OAuth2AuthorizedClientProviderBuilder;
import org.springframework.security.oauth2.client.registration.ClientRegistrationRepository;
import org.springframework.security.oauth2.client.web.DefaultOAuth2AuthorizedClientManager;
import org.springframework.security.oauth2.client.web.OAuth2AuthorizedClientRepository;
import org.springframework.security.oauth2.client.web.reactive.function.client.ServletOAuth2AuthorizedClientExchangeFilterFunction;
import org.springframework.security.oauth2.core.endpoint.OAuth2ParameterNames;
import org.springframework.util.StringUtils;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.web.servlet.config.annotation.EnableWebMvc;
import org.springframework.web.servlet.config.annotation.ResourceHandlerRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

import javax.servlet.http.HttpServletRequest;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.function.Function;

@Configuration
@EnableWebMvc
@EnableGlobalMethodSecurity(securedEnabled = true, prePostEnabled = true)
@EnableConfigurationProperties(OAuthConfig.class)
@Slf4j
public class ApplicationConfig implements WebMvcConfigurer {

   @Autowired
   private OAuthConfig oAuthConfig;

   @Autowired
   private ToolConfig toolConfig;

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

//   @Bean(name = "uaaRestTemplate")
//   public OAuth2RestTemplate uaaRestTemplate() {
//      ResourceOwnerPasswordResourceDetails resourceDetails = new ResourceOwnerPasswordResourceDetails();
//      resourceDetails.setClientId(oAuthConfig.getClientId());
//      resourceDetails.setClientSecret(oAuthConfig.getClientSecret());
//      resourceDetails.setUsername(oAuthConfig.getClientId());
//      resourceDetails.setPassword(oAuthConfig.getClientPassword());
//      resourceDetails.setAccessTokenUri(oAuthConfig.getAccessTokenUri());
//      resourceDetails.setClientAuthenticationScheme(AuthenticationScheme.form);
//
//      AccessTokenRequest atr = new DefaultAccessTokenRequest();
//      DefaultOAuth2ClientContext clientContext = new DefaultOAuth2ClientContext(atr);
//
//      OAuth2RestTemplate restTemplate = new OAuth2RestTemplate(resourceDetails, clientContext);
//      return restTemplate;
//   }

   @Bean(name = "uaaWebClient")
   WebClient webClient(OAuth2AuthorizedClientManager authorizedClientManager) {
      ServletOAuth2AuthorizedClientExchangeFilterFunction oauth2Client =
            new ServletOAuth2AuthorizedClientExchangeFilterFunction(authorizedClientManager);
      oauth2Client.setDefaultClientRegistrationId("uaa");
      return WebClient.builder()
            .apply(oauth2Client.oauth2Configuration())
            .build();
   }

   @Bean
   OAuth2AuthorizedClientManager authorizedClientManager(
         ClientRegistrationRepository clientRegistrationRepository,
         OAuth2AuthorizedClientRepository authorizedClientRepository) {

      OAuth2AuthorizedClientProvider authorizedClientProvider =
            OAuth2AuthorizedClientProviderBuilder.builder()
                  .authorizationCode()
                  .refreshToken()
                  .clientCredentials()
                  .password()
                  .build();
      DefaultOAuth2AuthorizedClientManager authorizedClientManager = new DefaultOAuth2AuthorizedClientManager(
            clientRegistrationRepository, authorizedClientRepository);
      authorizedClientManager.setAuthorizedClientProvider(authorizedClientProvider);

      // For the `password` grant, the `username` and `password` are supplied via request parameters,
      // so map it to `OAuth2AuthorizationContext.getAttributes()`.
      authorizedClientManager.setContextAttributesMapper(contextAttributesMapper());

      return authorizedClientManager;
   }

   private Function<OAuth2AuthorizeRequest, Map<String, Object>> contextAttributesMapper() {
      return authorizeRequest -> {
         Map<String, Object> contextAttributes = Collections.emptyMap();
         HttpServletRequest servletRequest = authorizeRequest.getAttribute(HttpServletRequest.class.getName());
         String username = servletRequest.getParameter(OAuth2ParameterNames.USERNAME);
         String password = servletRequest.getParameter(OAuth2ParameterNames.PASSWORD);
         if (StringUtils.hasText(username) && StringUtils.hasText(password)) {
            contextAttributes = new HashMap<>();

            // `PasswordOAuth2AuthorizedClientProvider` requires both attributes
            contextAttributes.put(OAuth2AuthorizationContext.USERNAME_ATTRIBUTE_NAME, username);
            contextAttributes.put(OAuth2AuthorizationContext.PASSWORD_ATTRIBUTE_NAME, password);
         }
         return contextAttributes;
      };
   }

   @Bean(name = "backgroundQueue")
   Queue backgroundQueue() {
      return new Queue(toolConfig.getBackgroundQueueName());
   }
}
