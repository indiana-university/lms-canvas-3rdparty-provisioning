package edu.iu.uits.lms.provisioning.controller.rest;

import edu.iu.uits.lms.provisioning.model.User;
import edu.iu.uits.lms.provisioning.repository.UserRepository;
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
@RequestMapping("/rest/dept_auth_user")
@Slf4j
@Api(tags = "dept_auth_user")
public class DeptAuthUserController {

   @Autowired
   private UserRepository userRepository;


   @GetMapping("/{id}")
   public User getFromId(@PathVariable Long id) {
      return userRepository.findById(id).orElse(null);
   }

   @GetMapping("/username/{username}")
   public User geByUsername(@PathVariable String username) {
      return userRepository.findByUsername(username);
   }

   @GetMapping("/canvasId/{canvasId}")
   public User getByCanvasUserId(@PathVariable String canvasId) {
      return userRepository.findByCanvasUserId(canvasId);
   }

   @GetMapping("/all")
   public List<User> getAll() {
      return (List<User>) userRepository.findAll();
   }

   @PutMapping("/{id}")
   public User update(@PathVariable Long id, @RequestBody User user) {
      User updatingUser = userRepository.findById(id).orElse(null);

      if (user.getGroupCode() != null) {
         updatingUser.setGroupCode(user.getGroupCode());
      }
      if (user.getUsername() != null) {
         updatingUser.setUsername(user.getUsername());
      }
      if (user.getCanvasUserId() != null) {
         updatingUser.setCanvasUserId(user.getCanvasUserId());
      }
      if (user.getDisplayName() != null) {
         updatingUser.setDisplayName(user.getDisplayName());
      }
      return userRepository.save(updatingUser);
   }

   @PostMapping("/")
   public User create(@RequestBody User user) {
      User newUser = new User();
      newUser.setGroupCode(user.getGroupCode());
      newUser.setUsername(user.getUsername());
      newUser.setDisplayName(user.getDisplayName());
      newUser.setCanvasUserId(user.getCanvasUserId());
      return userRepository.save(newUser);
   }

   @DeleteMapping("/{id}")
   public String delete(@PathVariable Long id) {
      userRepository.deleteById(id);
      return "Delete success.";
   }
}
