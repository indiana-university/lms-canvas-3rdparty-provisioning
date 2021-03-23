package edu.iu.uits.lms.provisioning.service;

import edu.iu.uits.lms.provisioning.model.content.FileContent;
import lombok.AllArgsConstructor;
import lombok.Data;

import java.io.Serializable;

/**
 * Object used to define an email message from preprocessing and the file path that the DeptRouter will use.
 * This object can be used to add more items in the future, if necessary.
 */
@Data
@AllArgsConstructor
public class ProvisioningResult {
    private StringBuilder emailMessage;
    private FileObject fileObject;
    private boolean hasException;

    private FileContent deferredProcessingData;

    private DeptRouter.CSV_TYPES deferredProcessingDataType;

    public ProvisioningResult(StringBuilder emailMessage, FileObject fileObject, boolean hasException) {
        this.emailMessage = emailMessage;
        this.fileObject = fileObject;
        this.hasException = hasException;
    }

    @Data
    @AllArgsConstructor
    public static class FileObject implements Serializable {
        private String fileName;
        private byte[] fileBytes;
    }

}
