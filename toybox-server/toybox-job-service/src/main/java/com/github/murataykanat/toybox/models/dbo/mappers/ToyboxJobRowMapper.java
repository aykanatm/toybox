package com.github.murataykanat.toybox.models.dbo.mappers;

import com.github.murataykanat.toybox.models.ToyboxJob;
import com.github.murataykanat.toybox.models.UploadFile;
import com.github.murataykanat.toybox.models.UploadFileLst;
import com.github.murataykanat.toybox.utilities.DateTimeUtils;
import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.springframework.jdbc.core.RowMapper;

import java.lang.reflect.Type;
import java.sql.ResultSet;
import java.sql.SQLException;
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
        toyboxJob.setStartTime(DateTimeUtils.stringToDate(resultSet.getString("START_TIME")));
        toyboxJob.setEndTime(DateTimeUtils.stringToDate(resultSet.getString("END_TIME")));
        toyboxJob.setStatus(resultSet.getString("STATUS"));

        Gson gson = new Gson();
        UploadFileLst uploadFileLst = gson.fromJson(resultSet.getString("PARAMETERS"), UploadFileLst.class);
        if(uploadFileLst != null){
            String username = uploadFileLst.getUploadFiles().get(0).getUsername();
            toyboxJob.setUsername(username);

            return toyboxJob;
        }
        else{
            throw new SQLException("Upload file list is null!");
        }
    }
}
