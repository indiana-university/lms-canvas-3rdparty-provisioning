package edu.iu.uits.lms.provisioning.config;

import com.nimbusds.jose.shaded.json.JSONArray;
import com.nimbusds.jose.shaded.json.JSONObject;
import edu.iu.uits.lms.iuonly.model.DeptProvisioningUser;
import edu.iu.uits.lms.iuonly.services.DeptProvisioningUserServiceImpl;
import edu.iu.uits.lms.lti.LTIConstants;
import edu.iu.uits.lms.lti.repository.DefaultInstructorRoleRepository;
import edu.iu.uits.lms.lti.service.LmsDefaultGrantedAuthoritiesMapper;
import edu.iu.uits.lms.lti.service.OidcTokenUtils;
import edu.iu.uits.lms.provisioning.controller.Constants;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.oauth2.core.oidc.user.OidcUserAuthority;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

@Slf4j
public class CustomRoleMapper extends LmsDefaultGrantedAuthoritiesMapper {

   private DeptProvisioningUserServiceImpl deptProvisioningUserService;

   public CustomRoleMapper(DefaultInstructorRoleRepository defaultInstructorRoleRepository, DeptProvisioningUserServiceImpl deptProvisioningUserService) {
      super(defaultInstructorRoleRepository);
      this.deptProvisioningUserService = deptProvisioningUserService;
   }

   @Override
   public Collection<? extends GrantedAuthority> mapAuthorities(Collection<? extends GrantedAuthority> authorities) {
      List<GrantedAuthority> remappedAuthorities = new ArrayList<>();
      remappedAuthorities.addAll(authorities);
      for (GrantedAuthority authority : authorities) {
         OidcUserAuthority userAuth = (OidcUserAuthority) authority;
         OidcTokenUtils oidcTokenUtils = new OidcTokenUtils(userAuth.getAttributes());
         log.debug("LTI Claims: {}", userAuth.getAttributes());

         String userId = oidcTokenUtils.getUserLoginId();

         String rolesString = "NotAuthorized";

         DeptProvisioningUser user = deptProvisioningUserService.findByUsername(userId);

         if (user != null) {
            rolesString = LTIConstants.CANVAS_INSTRUCTOR_ROLE;
         }

         String[] userRoles = rolesString.split(",");

         String newAuthString = returnEquivalentAuthority(userRoles, getDefaultInstructorRoles());


         if (user != null) {
            // Add a new custom claim
            JSONObject jsonObj = (JSONObject) userAuth.getIdToken().getClaims().get(LTIConstants.CLAIMS_KEY_CUSTOM);
            JSONArray groupCodes = new JSONArray();
            groupCodes.addAll(user.getGroupCode());
            jsonObj.put(Constants.AVAILABLE_GROUPS_KEY, groupCodes);
         }
         OidcUserAuthority newUserAuth = new OidcUserAuthority(newAuthString, userAuth.getIdToken(), userAuth.getUserInfo());
         remappedAuthorities.add(newUserAuth);
      }

      return remappedAuthorities;
   }
}
