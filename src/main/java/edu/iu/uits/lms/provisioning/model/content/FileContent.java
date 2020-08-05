package edu.iu.uits.lms.provisioning.model.content;

import java.io.Serializable;

public interface FileContent extends Serializable {
   String getFileName();
   Object getContents();
}
