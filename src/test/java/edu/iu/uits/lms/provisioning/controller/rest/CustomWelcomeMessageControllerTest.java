package edu.iu.uits.lms.provisioning.controller.rest;

/*-
 * #%L
 * lms-lti-3rdpartyprovisioning
 * %%
 * Copyright (C) 2015 - 2026 Indiana University
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

import edu.iu.uits.lms.provisioning.SharedMocks;
import edu.iu.uits.lms.provisioning.model.DeptAuthMessageSender;
import edu.iu.uits.lms.provisioning.service.CustomNotificationService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import static org.hamcrest.Matchers.containsString;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;


@WebMvcTest(controllers = CustomWelcomeMessageController.class)
@ContextConfiguration(classes = {CustomWelcomeMessageController.class})
@SharedMocks
class CustomWelcomeMessageControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private CustomNotificationService customNotificationService;

    private static final String TEST_USER = "testuser";


    @Test
    void testInvalidPropertiesFile() throws Exception {
        // Simulate invalid file by passing empty content
        MockMultipartFile file = new MockMultipartFile("messagePropertiesFile", "test.properties", "text/plain", "".getBytes());
        mockMvc.perform(multipart("/rest/dept_message_tester/validate")
                .file(file)
                .param("department", "math")
                .with(csrf())
                .with(user(TEST_USER)))
                .andExpect(status().isBadRequest())
                .andExpect(content().string(containsString("properties file has missing or invalid values")));
    }


    @Test
    void testUnauthorizedEmail() throws Exception {
        // Provide a more realistic properties file content
        String validProps = "sender=test@example.com\nsubject=Test\nbody=Hello";
        MockMultipartFile file = new MockMultipartFile("messagePropertiesFile", "test.properties", "text/plain", validProps.getBytes());
        when(customNotificationService.getValidatedCustomMessageSender(any(), eq("math"))).thenReturn(null);
        mockMvc.perform(multipart("/rest/dept_message_tester/validate")
                .file(file)
                .param("department", "math")
                .with(csrf())
                .with(user(TEST_USER)))
                .andExpect(status().isBadRequest())
                .andExpect(content().string(containsString("email specified in file is not authorized to send messages for the 'math' department")));
    }


    @Test
    void testValidRequest() throws Exception {
        // Provide a more realistic properties file content
        String validProps = "sender=test@example.com\nsubject=Test\nbody=Hello";
        MockMultipartFile file = new MockMultipartFile("messagePropertiesFile", "test.properties", "text/plain", validProps.getBytes());
        DeptAuthMessageSender sender = mock(DeptAuthMessageSender.class);
        when(sender.getEmail()).thenReturn("test@example.com");
        when(customNotificationService.getValidatedCustomMessageSender(any(), eq("math"))).thenReturn(sender);
        doNothing().when(customNotificationService).sendCustomWelcomeMessage(eq("test@example.com"), any());
        mockMvc.perform(multipart("/rest/dept_message_tester/validate")
                .file(file)
                .param("department", "math")
                .with(csrf())
                .with(user(TEST_USER)))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString("test email sent")));
    }
}
