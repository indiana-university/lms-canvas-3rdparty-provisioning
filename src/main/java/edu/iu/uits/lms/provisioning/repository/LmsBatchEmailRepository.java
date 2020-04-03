package edu.iu.uits.lms.provisioning.repository;

import edu.iu.uits.lms.provisioning.model.LmsBatchEmail;
import org.springframework.data.repository.PagingAndSortingRepository;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Component;

/**
 * Created by yingwang on 10/29/15.
 */

//@RepositoryRestResource(collectionResourceRel = "batchEmail", path = "batchEmail")
@Component
public interface LmsBatchEmailRepository extends PagingAndSortingRepository<LmsBatchEmail, Long> {

    /**
     * @param groupCode
     * @return LmsBathEmail
     */
    LmsBatchEmail getBatchEmailFromGroupCode(@Param("groupCode") String groupCode);

}
