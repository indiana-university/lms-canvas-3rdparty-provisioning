package edu.iu.uits.lms.provisioning;

import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.security.oauth2.client.OAuth2AuthorizedClientService;
import org.springframework.security.oauth2.client.registration.ClientRegistrationRepository;
import org.springframework.security.oauth2.jwt.JwtDecoder;

@TestConfiguration
public class TestAppConfig {

    @MockBean
    public JwtDecoder jwtDecoder;

    @MockBean
    private ClientRegistrationRepository clientRegistrationRepository;

    @MockBean
    private OAuth2AuthorizedClientService oAuth2AuthorizedClientService;
}