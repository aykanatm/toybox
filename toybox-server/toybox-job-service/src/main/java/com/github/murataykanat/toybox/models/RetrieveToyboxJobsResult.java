package com.github.murataykanat.toybox.models;

import com.fasterxml.jackson.annotation.JsonProperty;

import java.io.Serializable;
import java.util.List;

public class RetrieveToyboxJobsResult implements Serializable {
    @JsonProperty("jobs")
    private List<ToyboxJob> jobs;
    @JsonProperty("totalRecords")
    private int totalRecords;
    @JsonProperty("facets")
    private List<ToyboxJobFacet> facets;

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
}
