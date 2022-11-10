package edu.iu.uits.lms.provisioning;

import edu.iu.uits.lms.iuonly.services.DeptProvisioningUserServiceImpl;
import edu.iu.uits.lms.lti.LTIConstants;
import edu.iu.uits.lms.lti.config.TestUtils;
import edu.iu.uits.lms.provisioning.config.BackgroundMessageSender;
import edu.iu.uits.lms.provisioning.config.ToolConfig;
import edu.iu.uits.lms.provisioning.controller.ProvisioningController;
import edu.iu.uits.lms.provisioning.repository.DeptAuthMessageSenderRepository;
import edu.iu.uits.lms.provisioning.service.DeptRouter;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.web.context.WebApplicationContext;
import uk.ac.ox.ctl.lti13.security.oauth2.client.lti.authentication.OidcAuthenticationToken;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(controllers = ProvisioningController.class, properties = {"oauth.tokenprovider.url=http://foo"})
@Import(ToolConfig.class)
@ActiveProfiles("none")
public class AppLaunchSecurityTest {

   @Autowired
   private MockMvc mvc;

   @Autowired
   private WebApplicationContext context;

   @MockBean
   private DeptProvisioningUserServiceImpl deptProvisioningUserService;

   @MockBean
   private DeptAuthMessageSenderRepository deptAuthMessageSenderRepository;

   @MockBean
   private DeptRouter deptRouter;

   @MockBean
   private BackgroundMessageSender backgroundMessageSender;


   @Test
   public void appNoAuthnLaunch() throws Exception {
      //This is a secured endpoint and should not allow access without authn
      mvc.perform(get("/app/index")
            .header(HttpHeaders.USER_AGENT, TestUtils.defaultUseragent())
            .contentType(MediaType.APPLICATION_JSON))
            .andExpect(status().isForbidden());
   }

   @Test
   public void appAuthnWrongContextLaunch() throws Exception {
      OidcAuthenticationToken token = TestUtils.buildToken("userId",
            "asdf", LTIConstants.INSTRUCTOR_AUTHORITY);
      SecurityContextHolder.getContext().setAuthentication(token);

      //This is a secured endpoint and should not allow access without authn
      mvc.perform(get("/app/index")
            .header(HttpHeaders.USER_AGENT, TestUtils.defaultUseragent())
            .contentType(MediaType.APPLICATION_JSON))
            .andExpect(status().isOk());
   }

   @Test
   public void appAuthnLaunch() throws Exception {
      OidcAuthenticationToken token = TestUtils.buildToken("userId",
            "asdf", LTIConstants.INSTRUCTOR_AUTHORITY);
      SecurityContextHolder.getContext().setAuthentication(token);

      //This is a secured endpoint and should not allow access without authn
      mvc.perform(get("/app/index")
            .header(HttpHeaders.USER_AGENT, TestUtils.defaultUseragent())
            .contentType(MediaType.APPLICATION_JSON))
            .andExpect(status().isOk());
   }

   @Test
   public void randomUrlNoAuth() throws Exception {
      //This is a secured endpoint and should not allow access without authn
      mvc.perform(get("/asdf/foobar")
            .header(HttpHeaders.USER_AGENT, TestUtils.defaultUseragent())
            .contentType(MediaType.APPLICATION_JSON))
            .andExpect(status().isForbidden());
   }

   @Test
   public void randomUrlWithAuth() throws Exception {
      OidcAuthenticationToken token = TestUtils.buildToken("userId",
            "asdf", LTIConstants.INSTRUCTOR_AUTHORITY);
      SecurityContextHolder.getContext().setAuthentication(token);

      //This is a secured endpoint and should not allow access without authn
      mvc.perform(get("/asdf/foobar")
            .header(HttpHeaders.USER_AGENT, TestUtils.defaultUseragent())
            .contentType(MediaType.APPLICATION_JSON))
            .andExpect(status().isNotFound());
   }
}
