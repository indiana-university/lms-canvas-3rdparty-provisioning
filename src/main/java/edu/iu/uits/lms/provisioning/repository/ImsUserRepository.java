package edu.iu.uits.lms.provisioning.repository;

import edu.iu.uits.lms.provisioning.model.ImsUser;
import org.springframework.data.repository.PagingAndSortingRepository;
import org.springframework.stereotype.Component;

@Component
public interface ImsUserRepository extends PagingAndSortingRepository<ImsUser, String> {
   ImsUser findByLoginId(String loginId);
}
