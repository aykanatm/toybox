package com.github.murataykanat.toybox.models;

public class ToyboxJob {
    private String jobInstanceId;
    private String jobName;
    private String jobType;
    private String startTime;
    private String endTime;
    private String status;

    public String getJobInstanceId() {
        return jobInstanceId;
    }

    public void setJobInstanceId(String jobInstanceId) {
        this.jobInstanceId = jobInstanceId;
    }

    public String getJobName() {
        return jobName;
    }

    public void setJobName(String jobName) {
        this.jobName = jobName;
    }

    public String getStartTime() {
        return startTime;
    }

    public void setStartTime(String startTime) {
        this.startTime = startTime;
    }

    public String getEndTime() {
        return endTime;
    }

    public void setEndTime(String endTime) {
        this.endTime = endTime;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public String getJobType() {
        return jobType;
    }

    public void setJobType(String jobType) {
        if(jobType.equalsIgnoreCase("toybox-import"))
        {
            this.jobType = "IMPORT";
        }
        else if(jobType.equalsIgnoreCase("toybox-export"))
        {
            this.jobType = "EXPORT";
        }
        else
        {
            this.jobType = "CUSTOM";
        }
    }
}
