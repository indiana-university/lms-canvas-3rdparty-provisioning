package edu.iu.uits.lms.provisioning;

import edu.iu.uits.lms.provisioning.config.ToolConfig;
import edu.iu.uits.lms.provisioning.controller.rest.UploadRestController;
import edu.iu.uits.lms.provisioning.service.FileUploadService;
import lombok.extern.slf4j.Slf4j;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.AuthorityUtils;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.junit4.SpringRunner;
import org.springframework.test.web.servlet.MockMvc;

import java.io.IOException;
import java.io.InputStream;
import java.util.Collection;

import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.authentication;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@RunWith(SpringRunner.class)
@WebMvcTest(UploadRestController.class)
@Import(ToolConfig.class)
@Slf4j
@ActiveProfiles("none")
public class UploadRestControllerTest {

   @Autowired
   private MockMvc mvc;

   @MockBean
   private FileUploadService fileUploadService;

   @Test
   public void restNoAuthnLaunch() throws Exception {
      //This is a secured endpoint and should not not allow access without authn
      SecurityContextHolder.getContext().setAuthentication(null);
      mvc.perform(multipart("/rest/upload2/1234/zip")
            .file(getMockFile())
            .header(HttpHeaders.USER_AGENT, TestUtils.defaultUseragent()))
            .andExpect(status().isUnauthorized());
   }

   @Test
   public void restAuthnLaunch() throws Exception {
      Jwt jwt = TestUtils.createJwtToken("asdf");

      Collection<GrantedAuthority> authorities = AuthorityUtils.createAuthorityList("SCOPE_lms:rest", "ROLE_LMS_REST_ADMINS");
      JwtAuthenticationToken token = new JwtAuthenticationToken(jwt, authorities);

      //This is a secured endpoint and should not not allow access without authn
      mvc.perform(multipart("/rest/upload/1234/zip")
            .file(getMockFile())
            .with(authentication(token))
            .header(HttpHeaders.USER_AGENT, TestUtils.defaultUseragent()))
            .andExpect(status().isOk());
   }

   @Test
   public void restAuthnLaunchWithAlternateScope() throws Exception {
      Jwt jwt = TestUtils.createJwtToken("asdf");

      Collection<GrantedAuthority> authorities = AuthorityUtils.createAuthorityList("SCOPE_lms:prov:upload");
      JwtAuthenticationToken token = new JwtAuthenticationToken(jwt, authorities);

      //This is a secured endpoint and should not not allow access without authn
      mvc.perform(multipart("/rest/upload/1234/zip")
            .file(getMockFile())
            .with(authentication(token))
            .header(HttpHeaders.USER_AGENT, TestUtils.defaultUseragent()))
            .andExpect(status().isOk());
   }

   @Test
   public void restAuthnLaunchWithWrongScope() throws Exception {
      Jwt jwt = TestUtils.createJwtToken("asdf");

      Collection<GrantedAuthority> authorities = AuthorityUtils.createAuthorityList("SCOPE_read", "ROLE_NONE_YA");
      JwtAuthenticationToken token = new JwtAuthenticationToken(jwt, authorities);

      //This is a secured endpoint and should not not allow access without authn
      mvc.perform(post("/rest/upload/1234/zip")
            .header(HttpHeaders.USER_AGENT, TestUtils.defaultUseragent())
            .contentType(MediaType.MULTIPART_FORM_DATA)
            .with(authentication(token)))
            .andExpect(status().isForbidden());
   }

   private MockMultipartFile getMockFile() throws IOException {
      InputStream fileStream = this.getClass().getResourceAsStream("/prov_files.zip");
      MockMultipartFile file = new MockMultipartFile("deptFileUpload", "prov_files.zip", "application/zip", fileStream);
      return file;
   }
}
