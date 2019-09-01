package com.github.murataykanat.toybox.utilities;

import com.github.murataykanat.toybox.annotations.LogEntryExitExecutionTime;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.springframework.stereotype.Component;

import java.util.Comparator;
import java.util.List;

@Component
public class SortUtils{
    private static final Log _logger = LogFactory.getLog(SortUtils.class);

    @LogEntryExitExecutionTime
    public  <T> List<T> sortItems(String sortType, List<T> items, Comparator<T> comparing) {
        if(sortType.equalsIgnoreCase("des")){
            items.sort(comparing.reversed());
        }
        else if(sortType.equalsIgnoreCase("asc")){
            items.sort(comparing);
        }
        else{
            items.sort(comparing.reversed());
        }

        return items;
    }
}