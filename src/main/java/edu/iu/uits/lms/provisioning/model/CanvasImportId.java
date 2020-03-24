package edu.iu.uits.lms.provisioning.model;

import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.NonNull;
import lombok.RequiredArgsConstructor;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.Id;
import javax.persistence.PrePersist;
import javax.persistence.PreUpdate;
import javax.persistence.Table;
import java.util.Date;

/**
 * Created by chmaurer on 6/14/17.
 */
@Entity
@Table(name = "CANVAS_IMPORT_IDS")
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

    @Column(name = "ARCHIVE_PATH")
    @NonNull
    private String archivePath;

    @Column(name = "CREATEDON")
    private Date createdOn;

    @Column(name = "MODIFIEDON")
    private Date modifiedOn;

    @PreUpdate
    @PrePersist
    public void updateTimeStamps() {
        modifiedOn = new Date();
        if (createdOn==null) {
            createdOn = new Date();
        }
    }

}