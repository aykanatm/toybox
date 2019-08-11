package com.github.murataykanat.toybox.dbo;

import com.fasterxml.jackson.annotation.JsonProperty;

import javax.persistence.*;
import java.util.Date;

@Entity
@Table(name = "external_shares")
public class ExternalShare {
    @Id
    @Column(name = "external_share_id")
    @JsonProperty("externalShareId")
    private String externalShareId;

    @Column(name = "username")
    @JsonProperty("username")
    private String username;

    @Column(name = "job_id")
    @JsonProperty("jobId")
    private long jobId;

    @Column(name = "expiration_date")
    @JsonProperty("expirationDate")
    private Date expirationDate;

    @Column(name = "max_number_of_hits")
    @JsonProperty("maxNumberOfHits")
    private int maxNumberOfHits;

    @Column(name = "notify_when_downloaded")
    @JsonProperty("notifyWhenDownloaded")
    private String notifyWhenDownloaded;

    public String getExternalShareId() {
        return externalShareId;
    }

    public void setExternalShareId(String externalShareId) {
        this.externalShareId = externalShareId;
    }

    public String getUsername() {
        return username;
    }

    public void setUsername(String username) {
        this.username = username;
    }

    public long getJobId() {
        return jobId;
    }

    public void setJobId(long jobId) {
        this.jobId = jobId;
    }

    public Date getExpirationDate() {
        return expirationDate;
    }

    public void setExpirationDate(Date expirationDate) {
        this.expirationDate = expirationDate;
    }

    public int getMaxNumberOfHits() {
        return maxNumberOfHits;
    }

    public void setMaxNumberOfHits(int maxNumberOfHits) {
        this.maxNumberOfHits = maxNumberOfHits;
    }

    public String getNotifyWhenDownloaded() {
        return notifyWhenDownloaded;
    }

    public void setNotifyWhenDownloaded(String notifyWhenDownloaded) {
        this.notifyWhenDownloaded = notifyWhenDownloaded;
    }
}
