package edu.iu.uits.lms.provisioning.config;

import edu.iu.uits.lms.common.oauth.OAuthConfig;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import springfox.documentation.builders.AuthorizationCodeGrantBuilder;
import springfox.documentation.builders.OAuthBuilder;
import springfox.documentation.builders.PathSelectors;
import springfox.documentation.builders.RequestHandlerSelectors;
import springfox.documentation.service.AuthorizationScope;
import springfox.documentation.service.GrantType;
import springfox.documentation.service.SecurityReference;
import springfox.documentation.service.SecurityScheme;
import springfox.documentation.service.TokenEndpoint;
import springfox.documentation.service.TokenRequestEndpoint;
import springfox.documentation.spi.DocumentationType;
import springfox.documentation.spi.service.contexts.SecurityContext;
import springfox.documentation.spring.web.plugins.Docket;
import springfox.documentation.swagger.web.SecurityConfiguration;
import springfox.documentation.swagger.web.SecurityConfigurationBuilder;

import java.util.Arrays;

//@Configuration
//@EnableSwagger2
@Slf4j
public class SwaggerConfig {

   @Autowired
   private OAuthConfig oAuthConfig;

   @Bean
   public Docket api() {
      return new Docket(DocumentationType.SWAGGER_2)
            .select()
            .apis(RequestHandlerSelectors.basePackage("edu.iu.uits.lms.provisioning.controller.rest"))
            .paths(PathSelectors.any())
            .build()
            .securitySchemes(Arrays.asList(securityScheme()))
            .securityContexts(Arrays.asList(securityContext()));
   }

   @Bean
   public SecurityConfiguration security() {
      return SecurityConfigurationBuilder.builder()
            .clientId(oAuthConfig.getClientId())
            .clientSecret("CLIENT_SECRET")
            .scopeSeparator(" ")
            .useBasicAuthenticationWithAccessCodeGrant(false)
            .build();
   }

   private SecurityScheme securityScheme() {
//      GrantType grantType = new ResourceOwnerPasswordCredentialsGrant(oAuthConfig.getAccessTokenUri());
      GrantType grantType = new AuthorizationCodeGrantBuilder()
            .tokenEndpoint(new TokenEndpoint(oAuthConfig.getAccessTokenUri(), "oauthtoken"))
            .tokenRequestEndpoint(
                  new TokenRequestEndpoint(oAuthConfig.getUrl() + "/oauth/authorize",
                        oAuthConfig.getClientId(), oAuthConfig.getClientSecret()))
            .build();

      SecurityScheme oauth = new OAuthBuilder().name("spring_oauth")
            .grantTypes(Arrays.asList(grantType))
            .scopes(Arrays.asList(scopes()))
            .build();
      return oauth;
   }

   private AuthorizationScope[] scopes() {
      AuthorizationScope[] scopes = {
            new AuthorizationScope("lms:rest", "for lms rest operations") };
      return scopes;
   }

   private SecurityContext securityContext() {
      return SecurityContext.builder()
            .securityReferences(
                  Arrays.asList(new SecurityReference("spring_oauth", scopes())))
            .forPaths(PathSelectors.regex("/rest/*"))
            .build();
   }
}
