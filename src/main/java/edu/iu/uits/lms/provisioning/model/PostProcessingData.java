package edu.iu.uits.lms.provisioning.model;

import edu.iu.uits.lms.provisioning.model.content.FileContent;
import edu.iu.uits.lms.provisioning.repository.DataMapConverter;
import edu.iu.uits.lms.provisioning.service.DeptRouter;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.NonNull;
import lombok.RequiredArgsConstructor;
import org.apache.commons.collections4.MultiValuedMap;

import javax.persistence.Column;
import javax.persistence.Convert;
import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.Id;
import javax.persistence.Lob;
import javax.persistence.OneToOne;
import javax.persistence.SequenceGenerator;
import javax.persistence.Table;

@Data
@NoArgsConstructor
@RequiredArgsConstructor
@Entity
@Table(name = "DEPT_PROV_POST_PROCESSING")
@SequenceGenerator(name = "DEPT_PROV_POST_PROCESSING_ID_SEQ", sequenceName = "DEPT_PROV_POST_PROCESSING_ID_SEQ", allocationSize = 1)
public class PostProcessingData {
   @Id
   @GeneratedValue(generator = "DEPT_PROV_POST_PROCESSING_ID_SEQ")
   @Column(name = "DEPT_PROV_POST_PROCESSING_ID")
   private Long id;

   @Lob
   @Column(name = "DATA", columnDefinition="TEXT")
   @NonNull
   @Convert(converter = DataMapConverter.class)
   private MultiValuedMap<DeptRouter.CSV_TYPES, FileContent> postProcessingDataMap;

   @OneToOne(mappedBy = "postProcessingData")
   private CanvasImportId canvasImportId;

}
