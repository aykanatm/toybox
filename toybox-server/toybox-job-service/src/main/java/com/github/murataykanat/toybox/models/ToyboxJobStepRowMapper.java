package com.github.murataykanat.toybox.models;

import com.github.murataykanat.toybox.utilities.DateTimeUtils;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.springframework.jdbc.core.RowMapper;

import java.sql.ResultSet;
import java.sql.SQLException;

public class ToyboxJobStepRowMapper implements RowMapper<ToyboxJobStep> {
    private static final Log _logger = LogFactory.getLog(ToyboxJobStepRowMapper.class);

    @Override
    public ToyboxJobStep mapRow(ResultSet resultSet, int i) throws SQLException {
        ToyboxJobStep toyboxJobStep = new ToyboxJobStep();
        toyboxJobStep.setJobExecutionId(resultSet.getString("JOB_EXECUTION_ID"));
        toyboxJobStep.setStepExecutionId(resultSet.getString("STEP_EXECUTION_ID"));
        toyboxJobStep.setStepName(resultSet.getString("STEP_NAME"));
        toyboxJobStep.setStatus(resultSet.getString("STATUS"));
        toyboxJobStep.setStartTime(DateTimeUtils.stringToDate(resultSet.getString("START_TIME")));
        toyboxJobStep.setEndTime(DateTimeUtils.stringToDate(resultSet.getString("END_TIME")));
        return toyboxJobStep;
    }
}
