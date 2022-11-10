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

import edu.iu.uits.lms.provisioning.model.DeptProvArchive;
import edu.iu.uits.lms.provisioning.repository.ArchiveRepository;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.io.InputStreamResource;
import org.springframework.http.ContentDisposition;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.io.ByteArrayInputStream;

@RestController
@RequestMapping("/rest/archive")
@Slf4j
@Tag(name = "ArchiveRestController", description = "Interact with the DeptProvArchive repository to get archive details, and download files")
public class ArchiveRestController {

   @Autowired
   private ArchiveRepository archiveRepository = null;

   @GetMapping("/{id}")
   @Operation(summary = "Get a DeptProvArchive by id")
   public DeptProvArchive get(@PathVariable Long id) {
      DeptProvArchive deptProvArchive = archiveRepository.findById(id).orElse(null);
      if (deptProvArchive != null) {
         deptProvArchive.setCanvasContent(null);
         deptProvArchive.setOriginalContent(null);
      }
      return deptProvArchive;
   }

   @GetMapping("/importId/{id}")
   @Operation(summary = "Get a DeptProvArchive by Canvas Import id")
   public DeptProvArchive getByImport(@PathVariable String id) {
      DeptProvArchive deptProvArchive = archiveRepository.findByCanvasImportId(id);
      if (deptProvArchive != null) {
         deptProvArchive.setCanvasContent(null);
         deptProvArchive.setOriginalContent(null);
      }
      return deptProvArchive;
   }

   @GetMapping(value = "/download/{id}/original", produces = "application/zip")
   @Operation(summary = "Download a zip with the original upload files by id")
   public ResponseEntity downloadOriginal(@PathVariable(name = "id") Long id) {
      DeptProvArchive archive = archiveRepository.findById(id).orElse(null);

      if (archive != null) {
         return download(archive.getOriginalContent(), archive.getOriginalDisplayName());
      }
      return ResponseEntity.status(HttpStatus.NOT_FOUND).body("Cannot find file");
   }

   @GetMapping(value = "/download/{id}/canvas", produces = "application/zip")
   @Operation(summary = "Download a zip with the transformed files that were sent to Canvas by id")
   public ResponseEntity downloadCanvas(@PathVariable(name = "id") Long id) {
      DeptProvArchive archive = archiveRepository.findById(id).orElse(null);

      if (archive != null) {
         return download(archive.getCanvasContent(), archive.getCanvasDisplayName());
      }
      return ResponseEntity.status(HttpStatus.NOT_FOUND).body("Cannot find file");
   }

   private ResponseEntity<InputStreamResource> download(byte[] fileBytes, String fileName) {
      InputStreamResource resource = new InputStreamResource(new ByteArrayInputStream(fileBytes));

      HttpHeaders headers = new HttpHeaders();
      ContentDisposition cd = ContentDisposition.builder("attachment").filename(fileName).build();
      headers.setContentDisposition(cd);
//      headers.setContentDispositionFormData("attachment", fileName);

      return ResponseEntity.ok()
            .headers(headers)
            .contentLength(fileBytes.length)
            .contentType(new MediaType("application", "zip"))
            .body(resource);
   }

   @DeleteMapping("/{id}")
   @Operation(summary = "Delete a zDeptProvArchive by id")
   public String delete(@PathVariable Long id) {
      archiveRepository.deleteById(id);
      return "Delete success.";
   }

}
