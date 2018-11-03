package com.github.murataykanat.toybox.models;

import com.fasterxml.jackson.annotation.JsonProperty;

import java.io.Serializable;
import java.util.List;

public class RetrieveToyboxJobsResult implements Serializable {
    @JsonProperty("jobs")
    private List<ToyboxJob> jobs;
    @JsonProperty("totalRecords")
    private int totalRecords;

    public List<ToyboxJob> getJobs() {
        return jobs;
    }

    public void setJobs(List<ToyboxJob> jobs) {
        this.jobs = jobs;
    }

    public int getTotalRecords() {
        return totalRecords;
    }

    public void setTotalRecords(int totalRecords) {
        this.totalRecords = totalRecords;
    }
}
