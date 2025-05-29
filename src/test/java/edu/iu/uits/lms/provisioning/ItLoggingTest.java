package edu.iu.uits.lms.provisioning;

import edu.iu.uits.lms.audit.LmsIT12LoggerListener;
import edu.iu.uits.lms.common.cors.CorsSwaggerConfig;
import edu.iu.uits.lms.common.session.CourseSessionService;
import edu.iu.uits.lms.iuonly.services.SisServiceImpl;
import edu.iu.uits.lms.lti.config.TestUtils;
import edu.iu.uits.lms.lti.repository.DefaultInstructorRoleRepository;
import lombok.extern.slf4j.Slf4j;
import nl.altindag.log.LogCaptor;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Assumptions;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.actuate.autoconfigure.health.HealthContributorAutoConfiguration;
import org.springframework.boot.actuate.autoconfigure.health.HealthEndpointAutoConfiguration;
import org.springframework.boot.actuate.autoconfigure.mail.MailHealthContributorAutoConfiguration;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.context.metrics.buffering.BufferingApplicationStartup;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.AuthorityUtils;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import uk.ac.ox.ctl.lti13.nrps.NamesRoleService;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Collection;
import java.util.List;

import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.authentication;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest(classes = {WebApplication.class},
        properties = {"oauth.tokenprovider.url=http://foo", "lms.rabbitmq.queue_env_suffix=CI",
                "canvas.host=asdf", "canvas.token=asdf", "lti.errorcontact.name=asdf", "lti.errorcontact.link=asdf",
                "catalog.token=asdf", "spring.rabbitmq.listener.simple.auto-startup=false", "lms.swagger.cors.origin=123"})
@AutoConfigureMockMvc
@ActiveProfiles({"it12log", "swagger", "it12"})
@EnableAutoConfiguration(exclude = {HealthContributorAutoConfiguration.class, HealthEndpointAutoConfiguration.class,
        MailHealthContributorAutoConfiguration.class})
@AutoConfigureTestDatabase
@SharedMocks
@Slf4j
public class ItLoggingTest {
    @Autowired
    private MockMvc mvc;

    @MockitoBean
    private CourseSessionService courseSessionService;

    @MockitoBean
    private DefaultInstructorRoleRepository defaultInstructorRoleRepository;

    @MockitoBean
    private BufferingApplicationStartup bufferingApplicationStartup;

    @MockitoBean
    private NamesRoleService namesRoleService;

    @MockitoBean
    private CorsSwaggerConfig corsSwaggerConfig;

    @MockitoBean
    private SisServiceImpl sisService;

    @Autowired
    private LmsIT12LoggerListener lmsIT12LoggerListener;

    @Test
    public void testLmsEnhancementToIt12LogExistence() throws Exception {
        Assertions.assertNotNull(lmsIT12LoggerListener, "LmsIT12LoggerListener should be autowired");

        final String auditLoggerClassName = "edu.iu.es.esi.audit.AuditLogger";

        Class<?> clazz = null;

        try {
            clazz = Class.forName(auditLoggerClassName);
        } catch (ClassNotFoundException classNotFoundException) {
            log.info("Skipping test because AuditLogger not found");
        }

        Assumptions.assumeTrue(clazz != null);

        try (LogCaptor logCaptor = LogCaptor.forClass(clazz)) {
            final Jwt jwt = createJwtToken("asdf");

            final Collection<GrantedAuthority> authorities = AuthorityUtils.createAuthorityList("SCOPE_lms:rest", "ROLE_LMS_REST_ADMINS");
            final JwtAuthenticationToken token = new JwtAuthenticationToken(jwt, authorities);


            final String uriToCall = "/rest/archive/1234";

            mvc.perform(get(uriToCall)
                            .header(HttpHeaders.USER_AGENT, TestUtils.defaultUseragent())
                            .contentType(MediaType.APPLICATION_JSON)
                            .with(authentication(token)))
                    .andExpect(status().isOk());

            final List<String> it12LogEntries = logCaptor.getInfoLogs();

            Assertions.assertNotNull(it12LogEntries);
            Assertions.assertEquals(2, it12LogEntries.size());

            // The first log entry is the custom LMS log entry
            final String lmsIt12LogEntry = it12LogEntries.getFirst();

            Assertions.assertNotNull(lmsIt12LogEntry);
            Assertions.assertFalse(lmsIt12LogEntry.isEmpty());

            Assertions.assertTrue(lmsIt12LogEntry.contains("\"type\":\"successful authorization\""));
            Assertions.assertTrue(lmsIt12LogEntry.contains("\"user\":\"asdf\""));
            Assertions.assertTrue(lmsIt12LogEntry.contains("\"ipAddress\":\"127.0.0.1\""));
            Assertions.assertTrue(lmsIt12LogEntry.contains("\"message\":\"Successful authorization to uri " + uriToCall +
                    " as asdf with clientId asdf with audience [aud1, aud2] and authorities [LMS_REST_ADMINS] and scopes [lms:rest]\""));

            // The second log entry is the base IT12 log entry
            final String baseIt12LogEntry = it12LogEntries.getLast();

            Assertions.assertNotNull(baseIt12LogEntry);
            Assertions.assertFalse(baseIt12LogEntry.isEmpty());

            Assertions.assertTrue(baseIt12LogEntry.contains("\"type\":\"successful authorization\""));
            Assertions.assertTrue(baseIt12LogEntry.contains("\"user\":\"asdf\""));
            Assertions.assertTrue(baseIt12LogEntry.contains("\"ipAddress\":\"127.0.0.1\""));
            Assertions.assertTrue(baseIt12LogEntry.contains("\"message\":\"Successful access to " + uriToCall + "\""));
        }
    }

    public static Jwt createJwtToken(String client) {
        Jwt jwt = Jwt.withTokenValue("fake-token")
                .header("typ", "JWT")
//            .header("alg", SignatureAlgorithm.RS256.getValue())
                .claim("user_name", client)
                .claim("client_id", client)
                .audience(List.of("aud1", "aud2"))
                .notBefore(Instant.now())
                .expiresAt(Instant.now().plus(1, ChronoUnit.HOURS))
                .subject(client)
                .build();

        return jwt;
    }

}