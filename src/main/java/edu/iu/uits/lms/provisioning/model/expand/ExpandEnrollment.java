package edu.iu.uits.lms.provisioning.model.expand;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NonNull;

import java.io.Serializable;

@Data
@AllArgsConstructor
public class ExpandEnrollment implements Serializable {
    @NonNull
    @JsonProperty("canvas_user_id")
    private String canvasUserId;

    @NonNull
    @JsonProperty("listing_id")
    private String listingId;
}
