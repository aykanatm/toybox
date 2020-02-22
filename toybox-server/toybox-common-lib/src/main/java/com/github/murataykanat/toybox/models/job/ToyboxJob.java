package com.github.murataykanat.toybox.models.job;

import com.github.murataykanat.toybox.models.annotations.FacetColumnName;
import com.github.murataykanat.toybox.models.annotations.FacetDataType;
import com.github.murataykanat.toybox.models.annotations.FacetDefaultLookup;

import javax.persistence.*;
import java.util.Date;
import java.util.List;
@Entity
@Table(name = "TOYBOX_JOBS_VW")
public class ToyboxJob {
    @Id
    @Column(name = "JOB_INSTANCE_ID")
    private String jobInstanceId;
    @Column(name = "JOB_EXECUTION_ID")
    private String jobExecutionId;
    @Column(name = "JOB_NAME")
    @FacetColumnName(value = "Job Name")
    private String jobName;
    @Column(name = "JOB_TYPE")
    @FacetColumnName(value = "Job Type")
    private String jobType;
    @Column(name = "START_TIME")
    @FacetColumnName(value = "Start Time")
    @FacetDataType(value = "Date")
    @FacetDefaultLookup(values = {"Today","Past 7 days","Past 30 days"})
    private Date startTime;
    @Column(name = "END_TIME")
    @FacetColumnName(value = "End Time")
    @FacetDataType(value = "Date")
    @FacetDefaultLookup(values = {"Today","Past 7 days","Past 30 days"})
    private Date endTime;
    @Column(name = "STATUS")
    @FacetColumnName(value = "Status")
    private String status;
    @Column(name = "USERNAME")
    @FacetColumnName(value = "Username")
    private String username;
    @Transient
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
