package com.github.murataykanat.toybox.models;

import com.fasterxml.jackson.annotation.JsonProperty;

import java.io.Serializable;

public class ImportAssetResponse implements Serializable {
    @JsonProperty("jobId")
    private Long jobId;
    @JsonProperty("message")
    private String message;

    public Long getJobId() {
        return jobId;
    }

    public void setJobId(Long jobId) {
        this.jobId = jobId;
    }

    public String getMessage() {
        return message;
    }

    public void setMessage(String message) {
        this.message = message;
    }
}
