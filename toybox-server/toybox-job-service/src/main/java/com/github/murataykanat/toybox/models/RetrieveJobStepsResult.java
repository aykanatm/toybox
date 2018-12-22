package com.github.murataykanat.toybox.models;

import com.fasterxml.jackson.annotation.JsonProperty;

import java.io.Serializable;
import java.util.List;

public class RetrieveJobStepsResult implements Serializable {
    @JsonProperty("toyboxJobSteps")
    private List<ToyboxJobStep> toyboxJobSteps;
    @JsonProperty("message")
    private String message;

    public List<ToyboxJobStep> getToyboxJobSteps() {
        return toyboxJobSteps;
    }

    public void setToyboxJobSteps(List<ToyboxJobStep> toyboxJobSteps) {
        this.toyboxJobSteps = toyboxJobSteps;
    }

    public String getMessage() {
        return message;
    }

    public void setMessage(String message) {
        this.message = message;
    }
}
