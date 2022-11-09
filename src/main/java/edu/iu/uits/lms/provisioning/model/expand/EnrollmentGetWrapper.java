package edu.iu.uits.lms.provisioning.model.expand;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Data;

import java.io.Serializable;

@Data
@AllArgsConstructor
public class EnrollmentGetWrapper implements Serializable {
    @JsonProperty("listing_id")
    private String listingId;
}
