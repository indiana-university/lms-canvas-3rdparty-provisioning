package edu.iu.uits.lms.provisioning.model.expand;

import lombok.AllArgsConstructor;
import lombok.Data;

import java.io.Serializable;

@Data
@AllArgsConstructor
public class EnrollmentPostWrapper implements Serializable {
    private ExpandEnrollment enrollment;
}
