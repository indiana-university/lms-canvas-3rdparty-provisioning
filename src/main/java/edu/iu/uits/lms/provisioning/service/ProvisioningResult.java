package edu.iu.uits.lms.provisioning.service;

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

    @Data
    @AllArgsConstructor
    public static class FileObject implements Serializable {
        private String fileName;
        private byte[] fileBytes;
    }

}
