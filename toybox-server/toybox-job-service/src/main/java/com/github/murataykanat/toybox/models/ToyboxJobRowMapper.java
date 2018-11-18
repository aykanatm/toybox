package com.github.murataykanat.toybox.models;

import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;
import org.apache.commons.lang.StringUtils;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.springframework.jdbc.core.RowMapper;

import java.lang.reflect.Type;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;

public class ToyboxJobRowMapper implements RowMapper<ToyboxJob> {
    private static final Log _logger = LogFactory.getLog(ToyboxJobRowMapper.class);

    @Override
    public ToyboxJob mapRow(ResultSet resultSet, int i) throws SQLException {
        ToyboxJob toyboxJob = new ToyboxJob();
        toyboxJob.setJobInstanceId(resultSet.getString("JOB_INSTANCE_ID"));
        toyboxJob.setJobExecutionId(resultSet.getString("JOB_EXECUTION_ID"));
        toyboxJob.setJobName(resultSet.getString("JOB_NAME"));
        toyboxJob.setJobType(resultSet.getString("JOB_TYPE"));
        try {
            if(StringUtils.isBlank(resultSet.getString("START_TIME"))) {
                toyboxJob.setStartTime(null);
            }
            else {
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

        Gson gson = new Gson();
        Type listType = new TypeToken<ArrayList<UploadFile>>(){}.getType();
        ArrayList<UploadFile> uploadedFiles = gson.fromJson(resultSet.getString("PARAMETERS"), listType);
        String username = uploadedFiles.get(0).getUsername();
        toyboxJob.setUsername(username);

        return toyboxJob;
    }
}
