package edu.iu.uits.lms.provisioning.service;

import edu.iu.uits.lms.provisioning.model.content.FileContent;
import edu.iu.uits.lms.provisioning.model.content.StringArrayFileContent;
import lombok.extern.log4j.Log4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

@Log4j
@Service
public class CourseProvisioning {

    @Autowired
    private CsvService csvService = null;

    /**
     * Pass in a path to a csv file and a department code and this validate the data and send enrollments to Canvas
     * It is also assumed that if you're calling this method, you've passed in a valid Canvas course.csv header!
     * @param fileToProcess
     */
    public List<ProvisioningResult> processCourses(Collection<FileContent> fileToProcess) {
        List<ProvisioningResult> prs = new ArrayList<>();
        StringBuilder emailMessage = new StringBuilder();
        StringBuilder errorMessage = new StringBuilder();
        List<String[]> stringArrayList = new ArrayList<>();

        for (FileContent file : fileToProcess) {
            // read individual files line by line
            List <String[]> fileContents = ((StringArrayFileContent)file).getContents();
            emailMessage.append(file.getFileName() + ":\r\n");
            int rowCounter = 0;
            int headerLength = 0;

            for (String[] lineContentArray : fileContents) {
                if (rowCounter == 0) {
                    headerLength = lineContentArray.length;
                    rowCounter++;
                    stringArrayList.add(lineContentArray);
                    continue;
                }
                int lineLength = lineContentArray.length;

                if (lineLength != headerLength) {
                    rowCounter++;
                    errorMessage.append("\tLine " + rowCounter + " did not match the amount of fields specified in the header. Skipping. Double check the amount of commas and try again.\r\n");
                    continue;
                }

                stringArrayList.add(lineContentArray);
                rowCounter++;
            }

            StringBuilder finalMessage = new StringBuilder(emailMessage);
            InputStream inputStream = null;
            boolean fileException = false;
            try {
                // Create csv file to send to Canvas
                inputStream = csvService.writeCsvToStream(stringArrayList, null);

                if (errorMessage.length() > 0) {
                    finalMessage.append(errorMessage);
                    finalMessage.append("\tAll other entries were sent to Canvas.\r\n");
                } else {
                    finalMessage.append("\tAll entries were sent to Canvas.\r\n");
                }
            } catch (IOException e) {
                log.error("Error generating csv", e);
                finalMessage.append("\tThere were errors when generating the CSV file to send to Canvas\r\n");
                fileException = true;
            }

            ProvisioningResult pr = new ProvisioningResult(finalMessage,
                  new ProvisioningResult.FileObject(file.getFileName(), inputStream), fileException);
            prs.add(pr);
        }

        return prs;
    }
}
