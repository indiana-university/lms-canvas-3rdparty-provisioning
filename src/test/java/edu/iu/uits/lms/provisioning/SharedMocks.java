package edu.iu.uits.lms.provisioning;

import org.springframework.security.oauth2.client.OAuth2AuthorizedClientService;
import org.springframework.security.oauth2.client.registration.ClientRegistrationRepository;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.test.context.bean.override.mockito.MockitoBean;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

@Target(ElementType.TYPE)
@Retention(RetentionPolicy.RUNTIME)
@MockitoBean(types = {JwtDecoder.class, ClientRegistrationRepository.class, OAuth2AuthorizedClientService.class})
public @interface SharedMocks {

//    @MockBean
//    public JwtDecoder jwtDecoder;
//
//    @MockitoBean
//    public ClientRegistrationRepository clientRegistrationRepository;
//
//    @Bean
//    @Primary
//    public OAuth2AuthorizedClientService oAuth2AuthorizedClientService() {
//        return Mockito.mock(OAuth2AuthorizedClientService.class);
//    }
}