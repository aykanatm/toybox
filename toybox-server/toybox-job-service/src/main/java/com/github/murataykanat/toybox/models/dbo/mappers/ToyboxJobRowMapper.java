package com.github.murataykanat.toybox.models.dbo.mappers;

import com.github.murataykanat.toybox.models.ToyboxJob;
import com.github.murataykanat.toybox.utilities.DateTimeUtils;
import org.springframework.jdbc.core.RowMapper;

import java.sql.ResultSet;
import java.sql.SQLException;

public class ToyboxJobRowMapper implements RowMapper<ToyboxJob> {
    @Override
    public ToyboxJob mapRow(ResultSet resultSet, int i) throws SQLException {
        ToyboxJob toyboxJob = new ToyboxJob();
        toyboxJob.setJobInstanceId(resultSet.getString("JOB_INSTANCE_ID"));
        toyboxJob.setJobExecutionId(resultSet.getString("JOB_EXECUTION_ID"));
        toyboxJob.setJobName(resultSet.getString("JOB_NAME"));
        toyboxJob.setJobType(resultSet.getString("JOB_TYPE"));
        toyboxJob.setStartTime(DateTimeUtils.stringToDate(resultSet.getString("START_TIME")));
        toyboxJob.setEndTime(DateTimeUtils.stringToDate(resultSet.getString("END_TIME")));
        toyboxJob.setStatus(resultSet.getString("STATUS"));
        toyboxJob.setUsername(resultSet.getString("USERNAME"));

        return toyboxJob;
    }
}
