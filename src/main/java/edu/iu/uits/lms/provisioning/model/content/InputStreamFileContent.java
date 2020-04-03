package edu.iu.uits.lms.provisioning.model.content;

import lombok.AllArgsConstructor;
import lombok.Data;

import java.io.InputStream;

@Data
@AllArgsConstructor
public class InputStreamFileContent implements FileContent {
   private String fileName;
   private InputStream contents;
}
