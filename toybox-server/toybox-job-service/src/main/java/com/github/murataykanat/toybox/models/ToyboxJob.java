package com.github.murataykanat.toybox.models;

import com.github.murataykanat.toybox.models.annotations.FacetColumnName;
import com.github.murataykanat.toybox.models.annotations.FacetDefaultLookup;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import java.lang.reflect.Field;
import java.text.SimpleDateFormat;
import java.util.Calendar;
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
    @FacetDefaultLookup(values = {"Today","Past 7 days","Past 30 days"})
    private Date startTime;
    @FacetColumnName(value = "End Time")
    @FacetDefaultLookup(values = {"Today","Past 7 days","Past 30 days"})
    private Date endTime;
    @FacetColumnName(value = "Status")
    private String status;
    // TODO: Allow faceting based on username
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

        try{
            for(JobSearchRequestFacet jobSearchRequestFacet: jobSearchRequestFacetList){
                boolean hasFacet = false;
                for(Field field: this.getClass().getDeclaredFields()){
                    if(field.isAnnotationPresent(FacetColumnName.class)){
                        String fieldName = field.getAnnotation(FacetColumnName.class).value();
                        if(jobSearchRequestFacet.getFieldName().equalsIgnoreCase(fieldName)){
                            if(fieldName.equalsIgnoreCase("Start Time") || fieldName.equalsIgnoreCase("End Time")){
                                if(field.isAnnotationPresent(FacetDefaultLookup.class)){
                                    String fieldValue = jobSearchRequestFacet.getFieldValue();

                                    Calendar cal;
                                    cal = Calendar.getInstance();
                                    cal.set(Calendar.HOUR_OF_DAY, 0);
                                    cal.set(Calendar.MINUTE, 0);
                                    cal.set(Calendar.SECOND, 0);
                                    Date today = cal.getTime();

                                    SimpleDateFormat formatter = new SimpleDateFormat("MM/dd/yyyy HH:mm:ss");
                                    _logger.debug("Today: " + formatter.format(today));

                                    cal = Calendar.getInstance();
                                    cal.set(Calendar.HOUR_OF_DAY, 0);
                                    cal.set(Calendar.MINUTE, 0);
                                    cal.set(Calendar.SECOND, 0);
                                    cal.add(Calendar.DAY_OF_MONTH, 1);
                                    Date tomorrow = cal.getTime();
                                    _logger.debug("Tomorrow: " + formatter.format(tomorrow));

                                    cal = Calendar.getInstance();
                                    cal.set(Calendar.HOUR_OF_DAY, 0);
                                    cal.set(Calendar.MINUTE, 0);
                                    cal.set(Calendar.SECOND, 0);
                                    cal.add(Calendar.DAY_OF_MONTH, -7);
                                    Date sevenDaysAgo = cal.getTime();
                                    _logger.debug("Past 7 days: " + formatter.format(sevenDaysAgo));

                                    cal = Calendar.getInstance();
                                    cal.set(Calendar.HOUR_OF_DAY, 0);
                                    cal.set(Calendar.MINUTE, 0);
                                    cal.set(Calendar.SECOND, 0);
                                    cal.add(Calendar.DAY_OF_MONTH, -30);
                                    Date thirtyDaysAgo = cal.getTime();
                                    _logger.debug("Past 30 days: " + formatter.format(sevenDaysAgo));

                                    Date value = null;
                                    if(fieldName.equalsIgnoreCase("Start Time")){
                                        value = this.getStartTime();
                                    }
                                    else if(fieldName.equalsIgnoreCase("End Time")){
                                        value = this.getEndTime();
                                    }
                                    else{
                                        throw new Exception("Field name " + fieldName + " is not recognized.");
                                    }

                                    if(fieldValue.equalsIgnoreCase("Today")){
                                        if((value.after(today) || value.equals(today)) && value.before(tomorrow)){
                                            hasFacet = true;
                                            break;
                                        }
                                    }
                                    else if(fieldValue.equalsIgnoreCase("Past 7 days")){
                                        if(value.after(sevenDaysAgo) && value.before(tomorrow)){
                                            hasFacet = true;
                                            break;
                                        }
                                    }
                                    else if(fieldValue.equalsIgnoreCase("Past 30 days")){
                                        if(value.after(thirtyDaysAgo) && value.before(tomorrow)){
                                            hasFacet = true;
                                            break;
                                        }
                                    }
                                    else{
                                        throw new Exception("Field value " + fieldValue + " is not recognized.");
                                    }
                                }
                            }
                            else{
                                if(jobSearchRequestFacet.getFieldValue().equalsIgnoreCase((String) field.get(this)))
                                    hasFacet = true;
                                break;
                            }
                        }
                    }
                }

                result = result && hasFacet;
            }
        }
        catch (Exception e){
            String errorMessage = "An error occurred while determining if the job has the facet value. " + e.getLocalizedMessage();
            _logger.error(errorMessage, e);
        }
        return result;
    }

    public List<ToyboxJobStep> getSteps() {
        return steps;
    }

    public void setSteps(List<ToyboxJobStep> steps) {
        this.steps = steps;
    }
}
