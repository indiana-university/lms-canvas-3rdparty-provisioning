package edu.iu.uits.lms.provisioning.model.content;

import lombok.AllArgsConstructor;
import lombok.Data;

import java.util.List;

@Data
@AllArgsConstructor
public class CsvFileContent implements FileContent {
   private String fileName;
   private List<String[]> contents;
}
