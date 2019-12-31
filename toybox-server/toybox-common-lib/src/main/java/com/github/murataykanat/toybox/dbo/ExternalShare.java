package com.github.murataykanat.toybox.dbo;

import com.fasterxml.jackson.annotation.JsonProperty;

import javax.persistence.*;
import java.util.Date;

@Entity
@Table(name = "external_shares")
public class ExternalShare implements ShareItem{
    @Id
    @Column(name = "id")
    @JsonProperty("id")
    private String id;

    @Column(name = "username")
    @JsonProperty("username")
    private String username;

    @Column(name = "creation_date")
    @JsonProperty("creationDate")
    private Date creationDate;

    @Column(name = "job_id")
    @JsonProperty("jobId")
    private long jobId;

    @Column(name = "enable_expire")
    @JsonProperty("enableExpire")
    private String enableExpire;

    @Column(name = "expiration_date")
    @JsonProperty("expirationDate")
    private Date expirationDate;

    @Column(name = "enable_usage_limit")
    @JsonProperty("enableUsageLimit")
    private String enableUsageLimit;

    @Column(name = "max_number_of_hits")
    @JsonProperty("maxNumberOfHits")
    private int maxNumberOfHits;

    @Column(name = "notify_on_download")
    @JsonProperty("notifyOnDownload")
    private String notifyOnDownload;

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
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

    public String getNotifyOnDownload() {
        return notifyOnDownload;
    }

    public void setNotifyOnDownload(String notifyOnDownload) {
        this.notifyOnDownload = notifyOnDownload;
    }

    public String getEnableExpire() {
        return enableExpire;
    }

    public void setEnableExpire(String enableExpire) {
        this.enableExpire = enableExpire;
    }

    public String getEnableUsageLimit() {
        return enableUsageLimit;
    }

    public void setEnableUsageLimit(String enableUsageLimit) {
        this.enableUsageLimit = enableUsageLimit;
    }

    public Date getCreationDate() {
        return creationDate;
    }

    public void setCreationDate(Date creationDate) {
        this.creationDate = creationDate;
    }
}
