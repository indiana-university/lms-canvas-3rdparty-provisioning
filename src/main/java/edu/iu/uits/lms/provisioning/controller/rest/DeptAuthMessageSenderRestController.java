package edu.iu.uits.lms.provisioning.controller.rest;

import edu.iu.uits.lms.provisioning.model.DeptAuthMessageSender;
import edu.iu.uits.lms.provisioning.repository.DeptAuthMessageSenderRepository;
import io.swagger.annotations.Api;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/rest/dept_auth_sender")
@Slf4j
@Api(tags = "dept_auth_sender")
public class DeptAuthMessageSenderRestController {


   @Autowired
   private DeptAuthMessageSenderRepository deptAuthMessageSenderRepository;

   @GetMapping("/{id}")
   public DeptAuthMessageSender getFromId(@PathVariable Long id) {
      return deptAuthMessageSenderRepository.findById(id).orElse(null);
   }

   @GetMapping("/groupcode/{groupcode}")
   public List<DeptAuthMessageSender> getFromGroupCode(@PathVariable String groupcode) {
      return deptAuthMessageSenderRepository.findByGroupCodeIgnoreCase(groupcode);
   }

   @GetMapping("/email/{email}")
   public List<DeptAuthMessageSender> getFromEmail(@PathVariable String email) {
      return deptAuthMessageSenderRepository.findByEmailIgnoreCase(email);
   }

   @GetMapping("/all")
   public List<DeptAuthMessageSender> getAll() {
      return (List<DeptAuthMessageSender>) deptAuthMessageSenderRepository.findAll();
   }

   @PutMapping("/{id}")
   public DeptAuthMessageSender update(@PathVariable Long id, @RequestBody DeptAuthMessageSender deptAuthMessageSender) {
      DeptAuthMessageSender updatingSender = deptAuthMessageSenderRepository.findById(id).orElse(null);

      if (deptAuthMessageSender.getGroupCode() != null) {
         updatingSender.setGroupCode(deptAuthMessageSender.getGroupCode());
      }
      if (deptAuthMessageSender.getEmail() != null) {
         updatingSender.setEmail(deptAuthMessageSender.getEmail());
      }
      if (deptAuthMessageSender.getName() != null) {
         updatingSender.setName(deptAuthMessageSender.getName());
      }
      return deptAuthMessageSenderRepository.save(updatingSender);
   }

   @PostMapping("/")
   public DeptAuthMessageSender create(@RequestBody DeptAuthMessageSender deptAuthMessageSender) {
      DeptAuthMessageSender newSender = new DeptAuthMessageSender();
      newSender.setGroupCode(deptAuthMessageSender.getGroupCode());
      newSender.setEmail(deptAuthMessageSender.getEmail());
      newSender.setName(deptAuthMessageSender.getName());
      return deptAuthMessageSenderRepository.save(newSender);
   }

   @DeleteMapping("/{id}")
   public String delete(@PathVariable Long id) {
      deptAuthMessageSenderRepository.deleteById(id);
      return "Delete success.";
   }
}
