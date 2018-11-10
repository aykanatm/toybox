package com.github.murataykanat.toybox.models;

import org.apache.commons.lang.StringUtils;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.springframework.jdbc.core.RowMapper;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.text.ParseException;
import java.text.SimpleDateFormat;

public class ToyboxJobRowMapper implements RowMapper<ToyboxJob> {
    private static final Log _logger = LogFactory.getLog(ToyboxJobRowMapper.class);

    @Override
    public ToyboxJob mapRow(ResultSet resultSet, int i) throws SQLException {
        ToyboxJob toyboxJob = new ToyboxJob();
        toyboxJob.setJobInstanceId(resultSet.getString("JOB_INSTANCE_ID"));
        toyboxJob.setJobName(resultSet.getString("JOB_NAME"));
        toyboxJob.setJobType(resultSet.getString("JOB_NAME"));
        try {
            if(StringUtils.isBlank(resultSet.getString("START_TIME")))
            {
                toyboxJob.setStartTime(null);
            }
            else
            {
                toyboxJob.setStartTime(new SimpleDateFormat("yyyy-MM-dd HH:mm:ss.SSS").parse(resultSet.getString("START_TIME")));
            }
        }
        catch (ParseException e) {
            _logger.error("An error occured while parsing the date string " + resultSet.getString("START_TIME") + ". " + e.getLocalizedMessage(), e);
            toyboxJob.setStartTime(null);
        }
        try {
            if(StringUtils.isBlank(resultSet.getString("END_TIME"))){
                toyboxJob.setEndTime(null);
            }
            else{
                toyboxJob.setEndTime(new SimpleDateFormat("yyyy-MM-dd HH:mm:ss.SSS").parse(resultSet.getString("END_TIME")));
            }

        }
        catch (ParseException e) {
            _logger.error("An error occured while parsing the date string " + resultSet.getString("END_TIME") + ". " + e.getLocalizedMessage(), e);
            toyboxJob.setEndTime(null);
        }
        toyboxJob.setStatus(resultSet.getString("STATUS"));

        return toyboxJob;
    }
}
