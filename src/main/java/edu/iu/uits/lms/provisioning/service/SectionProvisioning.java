package edu.iu.uits.lms.provisioning.service;

import edu.iu.uits.lms.provisioning.model.content.ByteArrayFileContent;
import edu.iu.uits.lms.provisioning.model.content.FileContent;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

@Slf4j
@Service
public class SectionProvisioning {

   public List<ProvisioningResult> processSections(Collection<FileContent> fileToProcess) {
      List<ProvisioningResult> prs = new ArrayList<>();
      StringBuilder emailMessage = new StringBuilder();

      for (FileContent file : fileToProcess) {
         byte[] fileBytes = ((ByteArrayFileContent)file).getContents();
         emailMessage.append(file.getFileName() + ":\r\n");

         StringBuilder finalMessage = new StringBuilder();

         finalMessage.append(emailMessage);
         finalMessage.append("\tAll entries were sent to Canvas.\r\n");

         ProvisioningResult pr = new ProvisioningResult(finalMessage,
               new ProvisioningResult.FileObject(file.getFileName(), fileBytes), false);
         prs.add(pr);
      }

      return prs;
   }
}
