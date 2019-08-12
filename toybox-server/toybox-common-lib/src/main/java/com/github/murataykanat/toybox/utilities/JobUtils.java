package com.github.murataykanat.toybox.utilities;

import com.github.murataykanat.toybox.annotations.LogEntryExitExecutionTime;
import com.github.murataykanat.toybox.schema.job.RetrieveToyboxJobResult;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.ResponseEntity;
import org.springframework.web.client.RestTemplate;

import java.io.File;

public class JobUtils {
    private static final Log _logger = LogFactory.getLog(JobUtils.class);

    private static JobUtils jobUtils;

    private JobUtils(){}

    public static JobUtils getInstance(){
        if(jobUtils != null){
            return jobUtils;
        }

        jobUtils = new JobUtils();
        return jobUtils;
    }

    @LogEntryExitExecutionTime
    public File getArchiveFile(long jobId, HttpHeaders headers, String jobServiceUrl, String exportStagingPath) {
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
}
