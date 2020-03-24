package edu.iu.uits.lms.provisioning.repository;

import edu.iu.uits.lms.provisioning.model.CanvasImportId;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.PagingAndSortingRepository;
import org.springframework.data.repository.query.Param;
import org.springframework.transaction.annotation.Transactional;

import java.util.Date;
import java.util.List;

/**
 * Created by chmaurer on 6/14/17.
 */
public interface CanvasImportIdRepository extends PagingAndSortingRepository<CanvasImportId, String> {

   List<CanvasImportId> findByProcessedOrderByGroupCodeAscImportIdAsc(@Param("processed") String processed);

   @Modifying
   @Query("update CanvasImportId set processed = :processed, modifiedOn = :modifiedOn where importId = :importId")
   @Transactional("servicesTransactionManager")
   int setProcessedByImportId(@Param("processed") String processed, @Param("importId") String importId, @Param("modifiedOn") Date date);
}