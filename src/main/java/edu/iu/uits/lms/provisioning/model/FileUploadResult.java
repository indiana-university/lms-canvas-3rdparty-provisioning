package edu.iu.uits.lms.provisioning.model;

import edu.iu.uits.lms.provisioning.service.exception.FileError;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NonNull;
import lombok.RequiredArgsConstructor;

import java.io.Serializable;
import java.util.List;

@Data
@AllArgsConstructor
@RequiredArgsConstructor
public class FileUploadResult implements Serializable {
   @NonNull
   private String message;
   private List<FileError> fileErrors;
}
