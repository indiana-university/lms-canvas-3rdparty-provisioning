package edu.iu.uits.lms.provisioning.repository;

import edu.iu.uits.lms.provisioning.model.DeptAuthMessageSender;
import org.springframework.data.repository.PagingAndSortingRepository;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Component;

import java.util.List;

/**
 * Created by chmaurer on 9/11/19.
 */

@Component
public interface DeptAuthMessageSenderRepository extends PagingAndSortingRepository<DeptAuthMessageSender, Long> {

    List<DeptAuthMessageSender> findByGroupCodeIgnoreCase(@Param("groupCode") String groupCode);
    List<DeptAuthMessageSender> findByEmailIgnoreCase(@Param("email") String email);
    DeptAuthMessageSender findByGroupCodeIgnoreCaseAndEmailIgnoreCase(@Param("groupCode") String groupCode, @Param("email") String email);

}
