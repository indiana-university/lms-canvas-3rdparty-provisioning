package edu.iu.uits.lms.provisioning.controller.rest;

import edu.iu.uits.lms.provisioning.controller.Constants;
import edu.iu.uits.lms.provisioning.model.FileUploadResult;
import edu.iu.uits.lms.provisioning.service.DeptProvFileUploadService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.List;

@RestController
@RequestMapping("/rest/upload")
@Slf4j
@Tag(name = "UploadRestController", description = "Upload files for provisioning")
public class UploadRestController {

   @Autowired
   private DeptProvFileUploadService fileUploadService;

   @PostMapping("/{dept}/zip")
   @Operation(summary = "Upload a zip file containing individual files to provision")
   public ResponseEntity<FileUploadResult> uploadZip(@PathVariable("dept") String dept, @RequestParam("deptFileUpload") MultipartFile file,
                         @RequestParam(value = "customUsersNotification", required = false, defaultValue = "false") boolean customUsersNotification) {
      Object principal = SecurityContextHolder.getContext().getAuthentication().getPrincipal();
      try {
         List<MultipartFile> files = fileUploadService.unzip(file);
         return fileUploadService.parseFiles(files.toArray(new MultipartFile[0]), customUsersNotification, dept, principal, Constants.SOURCE.API);
      } catch (IOException e) {
         log.error("Unable to unzip uploaded file");
         FileUploadResult results = new FileUploadResult("Unable to unzip uploaded file");
         return ResponseEntity.badRequest().body(results);
      }
   }

   @PostMapping("/{dept}")
   @Operation(summary = "Upload files to provision")
   public ResponseEntity<FileUploadResult> upload(@PathVariable("dept") String dept, @RequestParam("deptFileUpload") MultipartFile[] files,
                                                  @RequestParam(value = "customUsersNotification", required = false, defaultValue = "false") boolean customUsersNotification) {
      Object principal = SecurityContextHolder.getContext().getAuthentication().getPrincipal();
      return fileUploadService.parseFiles(files, customUsersNotification, dept, principal, Constants.SOURCE.API);
   }

}
