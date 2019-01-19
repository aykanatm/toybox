package com.github.murataykanat.toybox.models.job;

import com.github.murataykanat.toybox.models.annotations.FacetColumnName;
import com.github.murataykanat.toybox.models.annotations.FacetDataType;
import com.github.murataykanat.toybox.models.annotations.FacetDefaultLookup;

import java.util.Date;
import java.util.List;

public class ToyboxJob {
    private String jobInstanceId;
    private String jobExecutionId;
    @FacetColumnName(value = "Job Name")
    private String jobName;
    @FacetColumnName(value = "Job Type")
    private String jobType;
    @FacetColumnName(value = "Start Time")
    @FacetDataType(value = "Date")
    @FacetDefaultLookup(values = {"Today","Past 7 days","Past 30 days"})
    private Date startTime;
    @FacetColumnName(value = "End Time")
    @FacetDataType(value = "Date")
    @FacetDefaultLookup(values = {"Today","Past 7 days","Past 30 days"})
    private Date endTime;
    @FacetColumnName(value = "Status")
    private String status;
    @FacetColumnName(value = "Username")
    private String username;
    private List<ToyboxJobStep> steps;

    public String getJobInstanceId() {
        return jobInstanceId;
    }

    public void setJobInstanceId(String jobInstanceId) {
        this.jobInstanceId = jobInstanceId;
    }

    @FacetColumnName(value = "Job Name")
    public String getJobName() {
        return jobName;
    }

    public void setJobName(String jobName) {
        this.jobName = jobName;
    }

    @FacetColumnName(value = "Start Time")
    @FacetDefaultLookup(values = {"Today","Past 7 days","Past 30 days"})
    public Date getStartTime() {
        return startTime;
    }

    public void setStartTime(Date startTime) {
        this.startTime = startTime;
    }

    @FacetColumnName(value = "End Time")
    @FacetDefaultLookup(values = {"Today","Past 7 days","Past 30 days"})
    public Date getEndTime() {
        return endTime;
    }

    public void setEndTime(Date endTime) {
        this.endTime = endTime;
    }

    @FacetColumnName(value = "Status")
    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    @FacetColumnName(value = "Job Type")
    public String getJobType() {
        return jobType;
    }

    public void setJobType(String jobType) { this.jobType = jobType; }

    @FacetColumnName(value = "Username")
    public String getUsername() {
        return username;
    }

    public void setUsername(String username) {
        this.username = username;
    }

    public String getJobExecutionId() {
        return jobExecutionId;
    }

    public void setJobExecutionId(String jobExecutionId) {
        this.jobExecutionId = jobExecutionId;
    }

    public List<ToyboxJobStep> getSteps() {
        return steps;
    }

    public void setSteps(List<ToyboxJobStep> steps) {
        this.steps = steps;
    }
}
