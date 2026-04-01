package edu.iu.uits.lms.provisioning.controller.rest;

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
