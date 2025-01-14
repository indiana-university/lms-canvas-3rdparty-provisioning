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

public interface Constants {
   String AVAILABLE_GROUPS_KEY = "groups";

   enum SOURCE {
      API,
      APP
   }

   String AUTH_USER_TOOL_PERMISSION = "DEPT_PROV";
   String AUTH_USER_TOOL_PERM_PROP_GROUP_CODES = "DEPT_PROV_GROUP_CODES";
   String AUTH_USER_TOOL_PERM_PROP_ALLOW_SIS_ENROLLMENTS = "DEPT_PROV_ALLOW_SIS_ENROLLMENTS";
   String AUTH_USER_TOOL_PERM_PROP_AUTHORIZED_ACCOUNTS = "DEPT_PROV_AUTHORIZED_ACCOUNTS";
   String AUTH_USER_TOOL_PERM_PROP_OVERRIDE_RESTRICTIONS = "DEPT_PROV_OVERRIDE_RESTRICTIONS";

}
