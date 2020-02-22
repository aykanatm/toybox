package com.github.murataykanat.toybox.utilities;

import com.github.murataykanat.toybox.annotations.LogEntryExitExecutionTime;
import com.github.murataykanat.toybox.contants.ToyboxConstants;
import com.github.murataykanat.toybox.models.job.QToyboxJob;
import com.github.murataykanat.toybox.models.job.ToyboxJob;
import com.github.murataykanat.toybox.predicates.ToyboxPredicateBuilder;
import com.github.murataykanat.toybox.predicates.ToyboxStringPath;
import com.github.murataykanat.toybox.repositories.JobsRepository;
import com.github.murataykanat.toybox.schema.job.RetrieveToyboxJobResult;
import com.github.murataykanat.toybox.schema.search.SearchCondition;
import com.google.common.collect.Lists;
import com.google.gson.Gson;
import com.google.gson.JsonObject;
import com.querydsl.core.types.OrderSpecifier;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Component;
import org.springframework.web.client.HttpStatusCodeException;
import org.springframework.web.client.RestTemplate;

import java.io.File;
import java.util.List;

@Component
public class JobUtils {
    private static final Log _logger = LogFactory.getLog(JobUtils.class);

    @Autowired
    private JobsRepository jobsRepository;

    @LogEntryExitExecutionTime
    @SuppressWarnings("unchecked")
    public List<ToyboxJob> getJobs(List<SearchCondition> searchConditions, String sortField, String sortType){
        OrderSpecifier<?> order;
        ToyboxPredicateBuilder<ToyboxJob> builder = new ToyboxPredicateBuilder().with(searchConditions, ToyboxJob.class);

        if(ToyboxConstants.SORT_TYPE_ASCENDING.equalsIgnoreCase(sortType)){
            if("startTime".equalsIgnoreCase(sortField)){
                order = QToyboxJob.toyboxJob.startTime.asc();
            }
            else if("endTime".equalsIgnoreCase(sortField)){
                order = QToyboxJob.toyboxJob.endTime.asc();
            }
            else{
                order = new ToyboxStringPath(QToyboxJob.toyboxJob, sortField).asc();
            }
        }
        else if(ToyboxConstants.SORT_TYPE_DESCENDING.equalsIgnoreCase(sortType)){
            if("startTime".equalsIgnoreCase(sortField)){
                order = QToyboxJob.toyboxJob.startTime.desc();
            }
            else if("endTime".equalsIgnoreCase(sortField)){
                order = QToyboxJob.toyboxJob.endTime.desc();
            }
            else{
                order = new ToyboxStringPath(QToyboxJob.toyboxJob, sortField).desc();
            }
        }
        else{
            throw new IllegalArgumentException("Sort type '" + sortType + "' is invalid!");
        }

        Iterable<ToyboxJob> iterableJobs = jobsRepository.findAll(builder.build(), order);
        return Lists.newArrayList(iterableJobs);
    }

    @LogEntryExitExecutionTime
    public File getArchiveFile(long jobId, HttpHeaders headers, String jobServiceUrl, String exportStagingPath) throws Exception {
        try{
            RestTemplate restTemplate = new RestTemplate();
            ResponseEntity<RetrieveToyboxJobResult> retrieveToyboxJobResultResponseEntity = restTemplate.exchange(jobServiceUrl + "/jobs/" + jobId, HttpMethod.GET, new HttpEntity<>(headers), RetrieveToyboxJobResult.class);
            RetrieveToyboxJobResult retrieveToyboxJobResult = retrieveToyboxJobResultResponseEntity.getBody();
            if(retrieveToyboxJobResult.getToyboxJob().getStatus().equalsIgnoreCase("COMPLETED")){
                String downloadFilePath =  exportStagingPath + File.separator + jobId + File.separator + "Download.zip";
                return new File(downloadFilePath);
            }
            else if(retrieveToyboxJobResult.getToyboxJob().getStatus().equalsIgnoreCase("FAILED")){
                _logger.info("Job with ID '" + jobId + "' failed.");
                return null;
            }
            else{
                return getArchiveFile(jobId, headers, jobServiceUrl, exportStagingPath);
            }
        }
        catch (HttpStatusCodeException e){
            JsonObject responseJson = new Gson().fromJson(e.getResponseBodyAsString(), JsonObject.class);
            throw new Exception("Download request was successful but the packaging job failed to start. " + responseJson.get("message").getAsString());
        }
    }
}
