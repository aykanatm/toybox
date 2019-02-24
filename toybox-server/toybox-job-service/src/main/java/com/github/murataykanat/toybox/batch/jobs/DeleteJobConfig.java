package com.github.murataykanat.toybox.batch.jobs;

import com.github.murataykanat.toybox.batch.utils.Constants;
import com.github.murataykanat.toybox.dbo.Asset;
import com.github.murataykanat.toybox.repositories.AssetsRepository;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.springframework.batch.core.*;
import org.springframework.batch.core.configuration.annotation.EnableBatchProcessing;
import org.springframework.batch.core.configuration.annotation.JobBuilderFactory;
import org.springframework.batch.core.configuration.annotation.StepBuilderFactory;
import org.springframework.batch.core.launch.support.RunIdIncrementer;
import org.springframework.batch.core.scope.context.ChunkContext;
import org.springframework.batch.core.step.tasklet.Tasklet;
import org.springframework.batch.repeat.RepeatStatus;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.cloud.context.config.annotation.RefreshScope;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.List;
import java.util.Map;

@RefreshScope
@Configuration
@EnableBatchProcessing
public class DeleteJobConfig {
    private static final Log _logger = LogFactory.getLog(DeleteJobConfig.class);

    @Autowired
    private AssetsRepository assetsRepository;

    @Bean
    public Job deleteJob(JobBuilderFactory jobBuilderFactory, StepBuilderFactory stepBuilderFactory){
        Step stepCompressAssets = stepBuilderFactory.get(Constants.STEP_DELETE_SET_ASSETS_AS_DELETED)
                .tasklet(new Tasklet() {
                    @Override
                    public RepeatStatus execute(StepContribution stepContribution, ChunkContext chunkContext) throws Exception {
                        _logger.debug("execute() >> [" + Constants.STEP_DELETE_SET_ASSETS_AS_DELETED + "]");

                        Map<String, Object> jobParameters = chunkContext.getStepContext().getJobParameters();
                        for(Map.Entry<String, Object> jobParameter: jobParameters.entrySet()){
                            if(jobParameter.getKey().startsWith(Constants.JOB_PARAM_DELETE_ASSET_ID)){
                                String assetId = (String) jobParameter.getValue();
                                // jdbcTemplate.update("UPDATE assets SET deleted=? WHERE asset_id=?",new Object[]{"Y", assetId});
                                assetsRepository.deleteAssetById("Y", assetId);
                            }
                        }

                        _logger.debug("<< execute() " + Constants.STEP_DELETE_SET_ASSETS_AS_DELETED + "]");
                        return RepeatStatus.FINISHED;
                    }
                })
                .build();

        return jobBuilderFactory.get(Constants.JOB_DELETE_NAME)
                .incrementer(new RunIdIncrementer())
                .validator(deleteValidator())
                .flow(stepCompressAssets)
                .end()
                .build();
    }

    private JobParametersValidator deleteValidator(){
        return new JobParametersValidator(){
            @Override
            public void validate(JobParameters parameters) throws JobParametersInvalidException {
                _logger.debug("validate() >>");

                Map<String, JobParameter> parameterMap = parameters.getParameters();
                for(Map.Entry<String, JobParameter> parameterEntry: parameterMap.entrySet()){
                    if(parameterEntry.getKey().startsWith(Constants.JOB_PARAM_DELETE_ASSET_ID)){
                        String assetId = (String) parameterEntry.getValue().getValue();
                        List<Asset> assets = assetsRepository.getAssetsById(assetId);
                        if(assets != null){
                            if(assets.size() == 1){
                                Asset asset = assets.get(0);
                                if(asset == null){
                                    throw new JobParametersInvalidException("Asset is null!");
                                }
                            }
                            else{
                                throw new JobParametersInvalidException("There are multiple assets with ID '" + assetId + "'");
                            }
                        }
                        else{
                            throw new JobParametersInvalidException("Asset ID '" + assetId + "' cannot be found in the database!");
                        }
                    }
                }

                _logger.debug("<< validate()");
            }
        };
    }
}
