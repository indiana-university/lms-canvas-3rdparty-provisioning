package edu.iu.uits.lms.provisioning.model;

import edu.iu.uits.lms.provisioning.controller.Constants;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.NonNull;
import lombok.RequiredArgsConstructor;

import javax.persistence.CascadeType;
import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.EnumType;
import javax.persistence.Enumerated;
import javax.persistence.Id;
import javax.persistence.JoinColumn;
import javax.persistence.OneToOne;
import javax.persistence.PrePersist;
import javax.persistence.PreUpdate;
import javax.persistence.Table;
import java.util.Date;

/**
 * Created by chmaurer on 6/14/17.
 */
@Entity
@Table(name = "DEPT_PROV_IMPORT_IDS")
@Data
@NoArgsConstructor
@RequiredArgsConstructor
public class CanvasImportId {

    @Id
    @Column(name = "IMPORT_ID")
    @NonNull
    private String importId;

    @Column(name = "PROCESSED")
    @NonNull
    private String processed;

    @Column(name = "GROUP_CODE")
    @NonNull
    private String groupCode;

    @Column(name = "SOURCE")
    @Enumerated(EnumType.STRING)
    @NonNull
    private Constants.SOURCE source;

    @Column(name = "CREATEDON")
    private Date createdOn;

    @Column(name = "MODIFIEDON")
    private Date modifiedOn;

    @OneToOne(cascade = CascadeType.ALL, targetEntity = PostProcessingData.class)
    @JoinColumn(name = "DEPT_PROV_POST_PROCESSING_ID", referencedColumnName = "DEPT_PROV_POST_PROCESSING_ID")
    private PostProcessingData postProcessingData;

    @PreUpdate
    @PrePersist
    public void updateTimeStamps() {
        modifiedOn = new Date();
        if (createdOn==null) {
            createdOn = new Date();
        }
    }

}