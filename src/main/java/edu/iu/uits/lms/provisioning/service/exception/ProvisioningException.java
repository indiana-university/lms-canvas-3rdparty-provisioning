package edu.iu.uits.lms.provisioning.service.exception;

import java.util.List;

public abstract class ProvisioningException extends Exception {

   public ProvisioningException(String message) {
      super(message);
   }

   public abstract List<FileError> getFileErrors();
}
