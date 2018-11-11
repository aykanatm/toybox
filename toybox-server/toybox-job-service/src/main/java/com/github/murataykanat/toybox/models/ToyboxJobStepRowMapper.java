package com.github.murataykanat.toybox.models;

import org.apache.commons.lang.StringUtils;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.springframework.jdbc.core.RowMapper;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.text.ParseException;
import java.text.SimpleDateFormat;

public class ToyboxJobStepRowMapper implements RowMapper<ToyboxJobStep> {
    private static final Log _logger = LogFactory.getLog(ToyboxJobStepRowMapper.class);

    @Override
    public ToyboxJobStep mapRow(ResultSet resultSet, int i) throws SQLException {
        ToyboxJobStep toyboxJobStep = new ToyboxJobStep();
        toyboxJobStep.setJobExecutionId(resultSet.getString("JOB_EXECUTION_ID"));
        toyboxJobStep.setStepExecutionId(resultSet.getString("STEP_EXECUTION_ID"));
        toyboxJobStep.setStepName(resultSet.getString("STEP_NAME"));
        toyboxJobStep.setStatus(resultSet.getString("STATUS"));
        try {
            if(StringUtils.isBlank(resultSet.getString("START_TIME"))) {
                toyboxJobStep.setStartTime(null);
            }
            else {
                toyboxJobStep.setStartTime(new SimpleDateFormat("yyyy-MM-dd HH:mm:ss.SSS").parse(resultSet.getString("START_TIME")));
            }
        }
        catch (ParseException e) {
            _logger.error("An error occured while parsing the date string " + resultSet.getString("START_TIME") + ". " + e.getLocalizedMessage(), e);
            toyboxJobStep.setStartTime(null);
        }
        try {
            if(StringUtils.isBlank(resultSet.getString("END_TIME"))){
                toyboxJobStep.setEndTime(null);
            }
            else{
                toyboxJobStep.setEndTime(new SimpleDateFormat("yyyy-MM-dd HH:mm:ss.SSS").parse(resultSet.getString("END_TIME")));
            }
        }
        catch (ParseException e) {
            _logger.error("An error occured while parsing the date string " + resultSet.getString("END_TIME") + ". " + e.getLocalizedMessage(), e);
            toyboxJobStep.setEndTime(null);
        }
        return toyboxJobStep;
    }
}
