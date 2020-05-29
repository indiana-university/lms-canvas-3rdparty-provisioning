package edu.iu.uits.lms.provisioning.model.content;

import lombok.AllArgsConstructor;
import lombok.Data;

@Data
@AllArgsConstructor
public class ByteArrayFileContent implements FileContent {
   private String fileName;
   private byte[] contents;
}
