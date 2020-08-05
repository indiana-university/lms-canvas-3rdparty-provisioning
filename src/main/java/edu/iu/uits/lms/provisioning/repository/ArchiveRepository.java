package edu.iu.uits.lms.provisioning.repository;

import edu.iu.uits.lms.provisioning.model.DeptProvArchive;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.PagingAndSortingRepository;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.Date;

@Component
public interface ArchiveRepository extends PagingAndSortingRepository<DeptProvArchive, Long> {

   DeptProvArchive findByCanvasImportId(String canvasImportId);

   @Modifying
   @Query("DELETE FROM DeptProvArchive a where a.department = :dept and a.createdOn < :date")
   @Transactional
   int removeOlderThan(@Param("dept") String dept, @Param("date") Date date);
}
