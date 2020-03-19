package edu.iu.uits.lms.provisioning.service;

import edu.iu.uits.lms.provisioning.model.content.CsvFileContent;
import edu.iu.uits.lms.provisioning.model.content.FileContent;
import lombok.extern.log4j.Log4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.io.File;
import java.io.IOException;
import java.nio.file.Path;
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
     * @param destPath
     */
    public List<ProvisioningResult> processCourses(Collection<FileContent> fileToProcess, Path destPath) throws IOException {
        List<ProvisioningResult> prs = new ArrayList<>();
        StringBuilder emailMessage = new StringBuilder();
        StringBuilder errorMessage = new StringBuilder();
        List<String[]> stringArrayList = new ArrayList<>();

        for (FileContent file : fileToProcess) {
            // read individual files line by line
            List <String[]> fileContents = ((CsvFileContent)file).getContents();
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

            // Create csv file to send to Canvas
            String csvFilePath = writeCsv(stringArrayList, file.getFileName(), destPath);

            File finalFile = new File(csvFilePath);
            StringBuilder finalMessage = new StringBuilder();

            if (errorMessage.length() > 0) {
                finalMessage.append(emailMessage);
                finalMessage.append(errorMessage);
                finalMessage.append("\tAll other entries were sent to Canvas.\r\n");
            }
            else {
                finalMessage.append(emailMessage);
                finalMessage.append("\tAll entries were sent to Canvas.\r\n");
            }

            ProvisioningResult pr = new ProvisioningResult(finalMessage, finalFile);
            prs.add(pr);
        }

        return prs;
    }

    /**
     * @param stringArrayList
     * @param fileName
     * @return Returns a String that contains the file path of where the new csv file has been written
     */
    private String writeCsv(List<String[]> stringArrayList, String fileName, Path destPath) throws IOException {
        log.info("Preparing to write a csv of the courses to send to Canvas...");

        String fullPath = destPath.resolve(fileName).toString();

        csvService.writeCsv(stringArrayList, null, fullPath);

        log.info("The csv file at " + fullPath + " was created!");

        return fullPath;
    }
}
