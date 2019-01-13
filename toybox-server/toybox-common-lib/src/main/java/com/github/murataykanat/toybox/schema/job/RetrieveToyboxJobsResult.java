package com.github.murataykanat.toybox.schema.job;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.github.murataykanat.toybox.models.job.ToyboxJob;
import com.github.murataykanat.toybox.models.job.ToyboxJobFacet;

import java.io.Serializable;
import java.util.List;

public class RetrieveToyboxJobsResult implements Serializable {
    @JsonProperty("jobs")
    private List<ToyboxJob> jobs;
    @JsonProperty("totalRecords")
    private int totalRecords;
    @JsonProperty("facets")
    private List<ToyboxJobFacet> facets;
    @JsonProperty("message")
    private String message;

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

    public List<ToyboxJobFacet> getFacets() {
        return facets;
    }

    public void setFacets(List<ToyboxJobFacet> facets) {
        this.facets = facets;
    }

    public String getMessage() {
        return message;
    }

    public void setMessage(String message) {
        this.message = message;
    }
}
