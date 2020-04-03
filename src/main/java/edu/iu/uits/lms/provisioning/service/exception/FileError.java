package edu.iu.uits.lms.provisioning.service.exception;

import lombok.AllArgsConstructor;
import lombok.Data;

import java.io.Serializable;

@Data
@AllArgsConstructor
public class FileError implements Serializable {
   private String title;
   private String description;
}
