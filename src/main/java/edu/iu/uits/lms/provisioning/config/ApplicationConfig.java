package edu.iu.uits.lms.provisioning.config;

/*-
 * #%L
 * lms-lti-3rdpartyprovisioning
 * %%
 * Copyright (C) 2015 - 2022 Indiana University
 * %%
 * Redistribution and use in source and binary forms, with or without modification,
 * are permitted provided that the following conditions are met:
 * 
 * 1. Redistributions of source code must retain the above copyright notice, this
 *    list of conditions and the following disclaimer.
 * 
 * 2. Redistributions in binary form must reproduce the above copyright notice,
 *    this list of conditions and the following disclaimer in the documentation
 *    and/or other materials provided with the distribution.
 * 
 * 3. Neither the name of the Indiana University nor the names of its contributors
 *    may be used to endorse or promote products derived from this software without
 *    specific prior written permission.
 * 
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS" AND
 * ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE IMPLIED
 * WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE ARE DISCLAIMED.
 * IN NO EVENT SHALL THE COPYRIGHT HOLDER OR CONTRIBUTORS BE LIABLE FOR ANY DIRECT,
 * INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES (INCLUDING,
 * BUT NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES; LOSS OF USE,
 * DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND ON ANY THEORY OF
 * LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY, OR TORT (INCLUDING NEGLIGENCE
 * OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE OF THIS SOFTWARE, EVEN IF ADVISED
 * OF THE POSSIBILITY OF SUCH DAMAGE.
 * #L%
 */

import edu.iu.uits.lms.provisioning.Constants;
import edu.iu.uits.lms.provisioning.model.NotificationForm;
import edu.iu.uits.lms.provisioning.model.content.ByteArrayFileContent;
import edu.iu.uits.lms.provisioning.model.content.StringArrayFileContent;
import edu.iu.uits.lms.provisioning.service.DeptRouter;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.collections4.multimap.ArrayListValuedHashMap;
import org.springframework.amqp.core.Queue;
import org.springframework.amqp.support.converter.SimpleMessageConverter;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.oauth2.client.AuthorizedClientServiceOAuth2AuthorizedClientManager;
import org.springframework.security.oauth2.client.OAuth2AuthorizedClientManager;
import org.springframework.security.oauth2.client.OAuth2AuthorizedClientProvider;
import org.springframework.security.oauth2.client.OAuth2AuthorizedClientProviderBuilder;
import org.springframework.security.oauth2.client.OAuth2AuthorizedClientService;
import org.springframework.security.oauth2.client.registration.ClientRegistrationRepository;
import org.springframework.security.oauth2.client.web.reactive.function.client.ServletOAuth2AuthorizedClientExchangeFilterFunction;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.web.servlet.config.annotation.EnableWebMvc;
import org.springframework.web.servlet.config.annotation.ResourceHandlerRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;


@Configuration
@EnableWebMvc
@EnableMethodSecurity(securedEnabled = true, prePostEnabled = true)
@Slf4j
public class ApplicationConfig implements WebMvcConfigurer {

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

   @Bean(name = "uaaWebClient")
   WebClient webClient(OAuth2AuthorizedClientManager authorizedClientManager) {
      ServletOAuth2AuthorizedClientExchangeFilterFunction oauth2Client =
              new ServletOAuth2AuthorizedClientExchangeFilterFunction(authorizedClientManager);
      // Use the uaac, for client credentials
      oauth2Client.setDefaultClientRegistrationId("uaac");
      return WebClient.builder()
              .apply(oauth2Client.oauth2Configuration())
              .build();
   }

   @Bean
   OAuth2AuthorizedClientManager authorizedClientManager(
           ClientRegistrationRepository clientRegistrationRepository,
           OAuth2AuthorizedClientService clientService) {

      OAuth2AuthorizedClientProvider authorizedClientProvider =
              OAuth2AuthorizedClientProviderBuilder.builder()
                      .refreshToken()
                      .clientCredentials()
                      .build();
      AuthorizedClientServiceOAuth2AuthorizedClientManager authorizedClientManager =
              new AuthorizedClientServiceOAuth2AuthorizedClientManager(clientRegistrationRepository, clientService);
      authorizedClientManager.setAuthorizedClientProvider(authorizedClientProvider);

      return authorizedClientManager;
   }

   @Bean(name = "backgroundQueue")
   Queue backgroundQueue() {
      return new Queue(toolConfig.getBackgroundQueueName());
   }

   @Bean
   public SimpleMessageConverter converter() {
      SimpleMessageConverter converter = new SimpleMessageConverter();
      converter.addAllowedListPatterns(BackgroundMessage.class.getName(),
              NotificationForm.class.getName(),
              ArrayListValuedHashMap.class.getName(),
              DeptRouter.CSV_TYPES.class.getName(),
              java.lang.Enum.class.getName(),
              ByteArrayFileContent.class.getName(),
              StringArrayFileContent.class.getName(),
              java.util.LinkedList.class.getName(),
              Constants.SOURCE.class.getName());
      return converter;
   }
}
