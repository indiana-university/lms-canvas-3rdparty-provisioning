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

import edu.iu.uits.lms.iuonly.model.acl.AuthorizedUser;
import edu.iu.uits.lms.iuonly.services.AuthorizedUserService;
import edu.iu.uits.lms.lti.LTIConstants;
import edu.iu.uits.lms.lti.repository.DefaultInstructorRoleRepository;
import edu.iu.uits.lms.lti.service.LmsDefaultGrantedAuthoritiesMapper;
import edu.iu.uits.lms.lti.service.OidcTokenUtils;
import edu.iu.uits.lms.provisioning.Constants;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.oauth2.core.oidc.user.OidcUserAuthority;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Map;

import static edu.iu.uits.lms.provisioning.Constants.AUTH_USER_TOOL_PERMISSION;
import static edu.iu.uits.lms.provisioning.Constants.AUTH_USER_TOOL_PERM_PROP_GROUP_CODES;

@Slf4j
public class CustomRoleMapper extends LmsDefaultGrantedAuthoritiesMapper {

   private AuthorizedUserService authorizedUserService;

   public CustomRoleMapper(DefaultInstructorRoleRepository defaultInstructorRoleRepository, AuthorizedUserService authorizedUserService) {
      super(defaultInstructorRoleRepository);
      this.authorizedUserService = authorizedUserService;
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

         AuthorizedUser user = authorizedUserService.findByActiveUsernameAndToolPermission(userId, AUTH_USER_TOOL_PERMISSION);

         if (user != null) {
            rolesString = LTIConstants.CANVAS_INSTRUCTOR_ROLE;
         }

         String[] userRoles = rolesString.split(",");

         String newAuthString = returnEquivalentAuthority(userRoles, getDefaultInstructorRoles());


         if (user != null) {
            // Add a new custom claim
            Map<String, Object> jsonObj = (Map) userAuth.getAttributes().get(LTIConstants.CLAIMS_KEY_CUSTOM);
            String groupCodes = user.getToolPermissionProperties(AUTH_USER_TOOL_PERMISSION).get(AUTH_USER_TOOL_PERM_PROP_GROUP_CODES);
            jsonObj.put(Constants.AVAILABLE_GROUPS_KEY, AuthorizedUserService.convertPropertyToList(groupCodes));
         }
         OidcUserAuthority newUserAuth = new OidcUserAuthority(newAuthString, userAuth.getIdToken(), userAuth.getUserInfo());
         remappedAuthorities.add(newUserAuth);
      }

      return remappedAuthorities;
   }
}
