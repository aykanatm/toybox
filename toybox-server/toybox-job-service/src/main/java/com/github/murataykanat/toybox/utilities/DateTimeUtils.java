package com.github.murataykanat.toybox.utilities;

import org.apache.commons.lang.StringUtils;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import java.text.SimpleDateFormat;
import java.util.Date;

public class DateTimeUtils {
    private static final Log _logger = LogFactory.getLog(DateTimeUtils.class);

    public static Date stringToDate(String dateString){
        Date result = null;

        try{
            if(StringUtils.isNotBlank(dateString)){
                result = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss.SSS").parse(dateString);
            }
        }
        catch (Exception e){
            _logger.error("An error occured while parsing the date string " + dateString + ". " + e.getLocalizedMessage(), e);
        }
        return result;
    }
}
