package edu.iu.uits.lms.provisioning.controller.rest;

import edu.iu.uits.lms.provisioning.model.DeptProvArchive;
import edu.iu.uits.lms.provisioning.repository.ArchiveRepository;
import io.swagger.annotations.Api;
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
@Api(tags = "archive")
public class ArchiveRestController {

   @Autowired
   private ArchiveRepository archiveRepository = null;

   @GetMapping("/{id}")
   public DeptProvArchive get(@PathVariable Long id) {
      DeptProvArchive deptProvArchive = archiveRepository.findById(id).orElse(null);
      if (deptProvArchive != null) {
         deptProvArchive.setCanvasContent(null);
         deptProvArchive.setOriginalContent(null);
      }
      return deptProvArchive;
   }

   @GetMapping("/importId/{id}")
   public DeptProvArchive getByImport(@PathVariable String id) {
      DeptProvArchive deptProvArchive = archiveRepository.findByCanvasImportId(id);
      if (deptProvArchive != null) {
         deptProvArchive.setCanvasContent(null);
         deptProvArchive.setOriginalContent(null);
      }
      return deptProvArchive;
   }

   @GetMapping(value = "/download/{id}/original", produces = "application/zip")
   public ResponseEntity downloadOriginal(@PathVariable(name = "id") Long id) {
      DeptProvArchive archive = archiveRepository.findById(id).orElse(null);

      if (archive != null) {
         return download(archive.getOriginalContent(), archive.getOriginalDisplayName());
      }
      return ResponseEntity.status(HttpStatus.NOT_FOUND).body("Cannot find file");
   }

   @GetMapping(value = "/download/{id}/canvas", produces = "application/zip")
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
   public String delete(@PathVariable Long id) {
      archiveRepository.deleteById(id);
      return "Delete success.";
   }

}