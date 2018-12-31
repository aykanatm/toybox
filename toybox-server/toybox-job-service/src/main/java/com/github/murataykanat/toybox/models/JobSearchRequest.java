package com.github.murataykanat.toybox.models;

import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.List;

public class JobSearchRequest {
    @JsonProperty("limit")
    private int limit;
    @JsonProperty("offset")
    private int offset;
    @JsonProperty("sortType")
    private String sortType;
    @JsonProperty("sortColumn")
    private String sortColumn;
    @JsonProperty("username")
    private String username;
    @JsonProperty("jobSearchRequestFacetList")
    private List<JobSearchRequestFacet> jobSearchRequestFacetList;

    public int getLimit() {
        return limit;
    }

    public void setLimit(int limit) {
        this.limit = limit;
    }

    public int getOffset() {
        return offset;
    }

    public void setOffset(int offset) {
        this.offset = offset;
    }

    public String getSortType() {
        return sortType;
    }

    public void setSortType(String sortType) {
        this.sortType = sortType;
    }

    public String getSortColumn() {
        return sortColumn;
    }

    public void setSortColumn(String sortColumn) {
        this.sortColumn = sortColumn;
    }

    public String getUsername() {
        return username;
    }

    public void setUsername(String username) {
        this.username = username;
    }

    public List<JobSearchRequestFacet> getJobSearchRequestFacetList() {
        return jobSearchRequestFacetList;
    }

    public void setJobSearchRequestFacetList(List<JobSearchRequestFacet> jobSearchRequestFacetList) {
        this.jobSearchRequestFacetList = jobSearchRequestFacetList;
    }
}