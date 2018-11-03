package com.github.murataykanat.toybox.models;

import org.springframework.jdbc.core.RowMapper;

import java.sql.ResultSet;
import java.sql.SQLException;

public class ToyboxJobRowMapper implements RowMapper<ToyboxJob> {
    @Override
    public ToyboxJob mapRow(ResultSet resultSet, int i) throws SQLException {
        ToyboxJob toyboxJob = new ToyboxJob();
        toyboxJob.setJobInstanceId(resultSet.getString("JOB_INSTANCE_ID"));
        toyboxJob.setJobName(resultSet.getString("JOB_NAME"));
        toyboxJob.setJobType(resultSet.getString("JOB_NAME"));
        toyboxJob.setStartTime(resultSet.getString("START_TIME"));
        toyboxJob.setEndTime(resultSet.getString("END_TIME"));
        toyboxJob.setStatus(resultSet.getString("STATUS"));

        return toyboxJob;
    }
}
