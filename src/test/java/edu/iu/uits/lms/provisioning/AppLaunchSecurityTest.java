package edu.iu.uits.lms.provisioning;

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

import edu.iu.uits.lms.common.server.ServerInfo;
import edu.iu.uits.lms.common.session.CourseSessionService;
import edu.iu.uits.lms.iuonly.services.AuthorizedUserService;
import edu.iu.uits.lms.lti.LTIConstants;
import edu.iu.uits.lms.lti.config.TestUtils;
import edu.iu.uits.lms.lti.repository.DefaultInstructorRoleRepository;
import edu.iu.uits.lms.provisioning.config.BackgroundMessageSender;
import edu.iu.uits.lms.provisioning.config.SecurityConfig;
import edu.iu.uits.lms.provisioning.controller.ProvisioningController;
import edu.iu.uits.lms.provisioning.repository.DeptAuthMessageSenderRepository;
import edu.iu.uits.lms.provisioning.service.DeptRouter;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import uk.ac.ox.ctl.lti13.security.oauth2.client.lti.authentication.OidcAuthenticationToken;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(controllers = ProvisioningController.class, properties = {"oauth.tokenprovider.url=http://foo"})
@ContextConfiguration(classes = {ProvisioningController.class, SecurityConfig.class})
@ActiveProfiles("none")
@SharedMocks
public class AppLaunchSecurityTest {

   @Autowired
   private MockMvc mvc;

   @MockitoBean
   private DeptAuthMessageSenderRepository deptAuthMessageSenderRepository;

   @MockitoBean
   private DeptRouter deptRouter;

   @MockitoBean
   private BackgroundMessageSender backgroundMessageSender;

   @MockitoBean
   private CourseSessionService courseSessionService;

   @MockitoBean
   private DefaultInstructorRoleRepository defaultInstructorRoleRepository;

   @MockitoBean
   private AuthorizedUserService authorizedUserService;

   @MockitoBean(name = ServerInfo.BEAN_NAME)
   private ServerInfo serverInfo;


   @Test
   public void appNoAuthnLaunch() throws Exception {
      //This is a secured endpoint and should not allow access without authn
      mvc.perform(get("/app/launch")
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
      mvc.perform(get("/app/launch")
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
      mvc.perform(get("/app/launch")
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
