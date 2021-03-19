package edu.iu.uits.lms.provisioning.repository;

import edu.iu.uits.lms.provisioning.model.content.FileContent;
import edu.iu.uits.lms.provisioning.service.DeptRouter;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.collections4.MultiValuedMap;
import org.apache.commons.lang3.SerializationUtils;

import javax.persistence.AttributeConverter;
import java.io.Serializable;



/**
 * Converter for storing this MultiValuedMap in the database
 */
@Slf4j
public class DataMapConverter implements AttributeConverter<MultiValuedMap<DeptRouter.CSV_TYPES, FileContent>, byte[]> {

   @Override
   public byte[] convertToDatabaseColumn(MultiValuedMap<DeptRouter.CSV_TYPES, FileContent> data) {
      return SerializationUtils.serialize((Serializable) data);
   }

   @Override
   public MultiValuedMap<DeptRouter.CSV_TYPES, FileContent> convertToEntityAttribute(byte[] dataBytes) {
      return SerializationUtils.deserialize(dataBytes);
   }

}