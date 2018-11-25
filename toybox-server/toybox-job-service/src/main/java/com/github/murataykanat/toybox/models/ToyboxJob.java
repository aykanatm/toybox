package com.github.murataykanat.toybox.models;

import com.github.murataykanat.toybox.models.annotations.FacetColumnName;
import com.github.murataykanat.toybox.models.annotations.FacetDefaultLookup;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import java.lang.reflect.Field;
import java.util.Date;
import java.util.List;

public class ToyboxJob {
    private static final Log _logger = LogFactory.getLog(ToyboxJob.class);

    private String jobInstanceId;
    private String jobExecutionId;
    @FacetColumnName(value = "Job Name")
    private String jobName;
    @FacetColumnName(value = "Job Type")
    private String jobType;
    @FacetColumnName(value = "Start Time")
    private Date startTime;
    @FacetColumnName(value = "End Time")
    private Date endTime;
    @FacetColumnName(value = "Status")
    private String status;
    // TODO:
    // Allow faceting based on username
    private String username;

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
    @FacetDefaultLookup(values = "Today,Past 7 days,Past 30 days")
    public Date getStartTime() {
        return startTime;
    }

    public void setStartTime(Date startTime) {
        this.startTime = startTime;
    }

    @FacetColumnName(value = "End Time")
    @FacetDefaultLookup(values = "Today,Past 7 days,Past 30 days")
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

    public boolean hasFacetValue(List<JobSearchRequestFacet> jobSearchRequestFacetList){
        boolean result = true;

        // TODO:
        // Add time related facets
        try{
            for(JobSearchRequestFacet jobSearchRequestFacet: jobSearchRequestFacetList){
                boolean hasFacet = false;
                for(Field field: this.getClass().getDeclaredFields()){
                    if(field.isAnnotationPresent(FacetColumnName.class)){
                        String fieldName = field.getAnnotation(FacetColumnName.class).value();
                        if(jobSearchRequestFacet.getFieldName().equalsIgnoreCase(fieldName)){
                            if(jobSearchRequestFacet.getFieldValue().equalsIgnoreCase((String) field.get(this)))
                                hasFacet = true;
                            break;
                        }
                    }
                }

                result = result && hasFacet;
            }
        }
        catch (Exception e){
            String errorMessage = "An error occured while determining if the job has the facet value. " + e.getLocalizedMessage();
            _logger.error(errorMessage, e);
        }
        return result;
    }
}
