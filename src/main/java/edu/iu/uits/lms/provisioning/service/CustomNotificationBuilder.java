package edu.iu.uits.lms.provisioning.service;

import edu.iu.uits.lms.provisioning.model.NotificationForm;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.Properties;

@Slf4j
public class CustomNotificationBuilder {

   private static final String SENDER_KEY = "sender";
   private static final String SUBJECT_KEY = "subject";
   private static final String BODY_KEY = "body";

   @Getter
   private Properties properties;

   @Getter
   private boolean fileExists;

   @Getter
   private boolean fileValid;

   public CustomNotificationBuilder(String filePath) {
      try {
         properties = processOptionalMessage(filePath);
         fileExists = true;
         fileValid = validateFile();
      } catch (IOException e) {
         log.info("No file found at the following location: " + filePath);
      }
   }

   public CustomNotificationBuilder(InputStream inputStream) {
      try {
         properties = processOptionalMessage(inputStream);
         fileExists = true;
         fileValid = validateFile();
      } catch (IOException e) {
         log.info("Unable to read input stream");
      }
   }

   public CustomNotificationBuilder(NotificationForm notificationForm) {
      if (notificationForm != null) {
         properties = new Properties();
         properties.setProperty(SENDER_KEY, notificationForm.getSender());
         properties.setProperty(SUBJECT_KEY, notificationForm.getSubject());
         properties.setProperty(BODY_KEY, notificationForm.getBody());
         fileExists = true;
         fileValid = validateFile();
      }
   }

   public String getSender() {
      return properties.getProperty(SENDER_KEY);
   }

   public String getSubject() {
      return properties.getProperty(SUBJECT_KEY);
   }

   public String getBody() {
      return properties.getProperty(BODY_KEY);
   }

   private boolean validateFile() {
      return !StringUtils.isBlank(getSender()) && !StringUtils.isBlank(getSubject()) && !StringUtils.isBlank(getBody());
   }

   private Properties processOptionalMessage(String filePath) throws IOException {
      File file = new File(filePath);
      InputStream inputStream = new FileInputStream(file);
      return processOptionalMessage(inputStream);
   }
   
   private Properties processOptionalMessage(InputStream inputStream) throws IOException {
      CaselessProperties props = new CaselessProperties();
      props.load(inputStream);
      return props;
   }

   /**
    * Override Properties#put so that we can insert values where the case of the keys are always lowercased
    */
   private static class CaselessProperties extends Properties {
      public Object put(Object key, Object value) {
         String lowercase = ((String) key).toLowerCase();
         return super.put(lowercase, value);
      }
   }
}
