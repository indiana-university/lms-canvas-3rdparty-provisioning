package edu.iu.uits.lms.provisioning.service;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.File;

/**
 * Object used to define an email message from preprocessing and the file path that the DeptRouter will use.
 * This object can be used to add more items in the future, if necessary.
 */
@Data
@AllArgsConstructor
@NoArgsConstructor
public class ProvisioningResult {
    public StringBuilder emailMessage;
    public File file;
}
