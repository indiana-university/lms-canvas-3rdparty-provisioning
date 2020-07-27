package edu.iu.uits.lms.provisioning.controller.rest;

import edu.iu.uits.lms.provisioning.config.ToolConfig;
import io.swagger.annotations.Api;
import lombok.Data;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/rest/info")
@Slf4j
@Api(tags = "info")
public class InfoController {

   @Autowired
   private ToolConfig toolConfig;

   @GetMapping
   public Config getInfo() {
      return new Config(toolConfig);
   }

   /**
    * Need this class because I can't return the ToolConfig directly due to beanFactory things also being returned
    */
   @Data
   private static class Config {
      private String version;
      private String env;
      private String guestAccountCreationUrl;
      private String canvasServiceName;
      private String expandServiceName;

      public Config(ToolConfig toolConfig) {
         this.version = toolConfig.getVersion();
         this.env = toolConfig.getEnv();
         this.guestAccountCreationUrl = toolConfig.getGuestAccountCreationUrl();
         this.canvasServiceName = toolConfig.getCanvasServiceName();
         this.expandServiceName = toolConfig.getExpandServiceName();
      }
   }

}
