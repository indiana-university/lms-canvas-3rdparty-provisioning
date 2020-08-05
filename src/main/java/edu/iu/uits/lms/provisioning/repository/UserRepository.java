package edu.iu.uits.lms.provisioning.repository;

import edu.iu.uits.lms.provisioning.model.User;
import org.springframework.data.repository.PagingAndSortingRepository;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Component;

//import org.springframework.data.rest.core.annotation.RepositoryRestResource;

@Component
//@RepositoryRestResource(path = "users")
public interface UserRepository extends PagingAndSortingRepository<User, Long> {

   User findByUsername(@Param("username") String username);
   User findByCanvasUserId(@Param("canvasUserId") String canvasUserId);

}
