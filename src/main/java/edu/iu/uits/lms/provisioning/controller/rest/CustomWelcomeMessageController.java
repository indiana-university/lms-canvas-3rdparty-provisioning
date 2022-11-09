package edu.iu.uits.lms.provisioning.controller.rest;

import edu.iu.uits.lms.provisioning.model.DeptAuthMessageSender;
import edu.iu.uits.lms.provisioning.service.CustomNotificationBuilder;
import edu.iu.uits.lms.provisioning.service.CustomNotificationService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;

@RestController
@RequestMapping("/rest/dept_message_tester")
@Tag(name = "CustomWelcomeMessageController", description = "Validate details for a custom welcome message and send a test email message")
public class CustomWelcomeMessageController {

   @Autowired
   private CustomNotificationService customNotificationService = null;

   @PostMapping(value="/validate")
   @Operation(summary = "Validate the given properties and send a test email welcome message")
   public ResponseEntity validatePropertiesFile(@RequestParam("messagePropertiesFile") MultipartFile messagePropertiesFile,
                                                @RequestParam("department") String department) throws IOException {

      CustomNotificationBuilder cnb = new CustomNotificationBuilder(messagePropertiesFile.getInputStream());
      if (!cnb.isFileValid()) {
         return ResponseEntity.status(HttpStatus.BAD_REQUEST).body("properties file has missing or invalid values");
      }

      DeptAuthMessageSender validatedCustomMessageSender = customNotificationService.getValidatedCustomMessageSender(cnb, department);

      if (validatedCustomMessageSender != null) {
         customNotificationService.sendCustomWelcomeMessage(validatedCustomMessageSender.getEmail(), cnb);
         return ResponseEntity.ok("test email sent");
      }
      return ResponseEntity.status(HttpStatus.BAD_REQUEST).body("email specified in file is not authorized to send messages for the '" + department + "' department");
   }

}
