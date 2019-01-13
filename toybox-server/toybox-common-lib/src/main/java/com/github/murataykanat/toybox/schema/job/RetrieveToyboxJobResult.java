package com.github.murataykanat.toybox.schema.job;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.github.murataykanat.toybox.models.job.ToyboxJob;

import java.io.Serializable;

public class RetrieveToyboxJobResult implements Serializable {
    @JsonProperty("toyboxJob")
    private ToyboxJob toyboxJob;
    @JsonProperty("message")
    private String message;

    public ToyboxJob getToyboxJob() {
        return toyboxJob;
    }

    public void setToyboxJob(ToyboxJob toyboxJob) {
        this.toyboxJob = toyboxJob;
    }

    public String getMessage() {
        return message;
    }

    public void setMessage(String message) {
        this.message = message;
    }
}
