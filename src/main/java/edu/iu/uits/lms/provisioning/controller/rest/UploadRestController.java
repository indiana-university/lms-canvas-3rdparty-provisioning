package edu.iu.uits.lms.provisioning.controller.rest;

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
