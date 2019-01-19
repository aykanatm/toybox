package com.github.murataykanat.toybox.utilities;

import com.github.murataykanat.toybox.models.annotations.FacetColumnName;
import com.github.murataykanat.toybox.models.annotations.FacetDataType;
import com.github.murataykanat.toybox.models.annotations.FacetDefaultLookup;
import com.github.murataykanat.toybox.schema.common.SearchRequestFacet;
import org.apache.commons.beanutils.PropertyUtils;
import org.apache.commons.lang.reflect.FieldUtils;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import java.beans.PropertyDescriptor;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.List;

public class FacetUtils {
    private static final Log _logger = LogFactory.getLog(FacetUtils.class);

    private static FacetUtils facetUtils;

    private FacetUtils(){ }

    public static FacetUtils getInstance(){
        if(facetUtils != null){
            return facetUtils;
        }
        return new FacetUtils();
    }

    public boolean hasFacetValue(Object obj, List<SearchRequestFacet> searchRequestFacetList){
        boolean result = true;

        try{
            for(SearchRequestFacet searchRequestFacet: searchRequestFacetList){
                boolean hasFacet = false;
                for(Field field: obj.getClass().getDeclaredFields()){
                    if(field.isAnnotationPresent(FacetColumnName.class)){
                        String facetFieldName = field.getAnnotation(FacetColumnName.class).value();
                        if(searchRequestFacet.getFieldName().equalsIgnoreCase(facetFieldName)){
                            if(field.isAnnotationPresent(FacetDataType.class) && field.getAnnotation(FacetDataType.class).value().equalsIgnoreCase("Date")){
                                if(field.isAnnotationPresent(FacetDefaultLookup.class)){
                                    String fieldValue = searchRequestFacet.getFieldValue();

                                    Calendar cal;
                                    cal = Calendar.getInstance();
                                    cal.set(Calendar.HOUR_OF_DAY, 0);
                                    cal.set(Calendar.MINUTE, 0);
                                    cal.set(Calendar.SECOND, 0);
                                    Date today = cal.getTime();

                                    SimpleDateFormat formatter = new SimpleDateFormat("MM/dd/yyyy HH:mm:ss");
                                    _logger.debug("Today: " + formatter.format(today));

                                    cal = Calendar.getInstance();
                                    cal.set(Calendar.HOUR_OF_DAY, 0);
                                    cal.set(Calendar.MINUTE, 0);
                                    cal.set(Calendar.SECOND, 0);
                                    cal.add(Calendar.DAY_OF_MONTH, 1);
                                    Date tomorrow = cal.getTime();
                                    _logger.debug("Tomorrow: " + formatter.format(tomorrow));

                                    cal = Calendar.getInstance();
                                    cal.set(Calendar.HOUR_OF_DAY, 0);
                                    cal.set(Calendar.MINUTE, 0);
                                    cal.set(Calendar.SECOND, 0);
                                    cal.add(Calendar.DAY_OF_MONTH, -7);
                                    Date sevenDaysAgo = cal.getTime();
                                    _logger.debug("Past 7 days: " + formatter.format(sevenDaysAgo));

                                    cal = Calendar.getInstance();
                                    cal.set(Calendar.HOUR_OF_DAY, 0);
                                    cal.set(Calendar.MINUTE, 0);
                                    cal.set(Calendar.SECOND, 0);
                                    cal.add(Calendar.DAY_OF_MONTH, -30);
                                    Date thirtyDaysAgo = cal.getTime();
                                    _logger.debug("Past 30 days: " + formatter.format(sevenDaysAgo));

                                    Date value = (Date) PropertyUtils.getProperty(obj, field.getName());


                                    if(fieldValue.equalsIgnoreCase("Today")){
                                        if((value.after(today) || value.equals(today)) && value.before(tomorrow)){
                                            hasFacet = true;
                                            break;
                                        }
                                    }
                                    else if(fieldValue.equalsIgnoreCase("Past 7 days")){
                                        if(value.after(sevenDaysAgo) && value.before(tomorrow)){
                                            hasFacet = true;
                                            break;
                                        }
                                    }
                                    else if(fieldValue.equalsIgnoreCase("Past 30 days")){
                                        if(value.after(thirtyDaysAgo) && value.before(tomorrow)){
                                            hasFacet = true;
                                            break;
                                        }
                                    }
                                    else{
                                        throw new Exception("Lookup value " + fieldValue + " is not recognized.");
                                    }
                                }
                            }
                            else{

                                if(searchRequestFacet.getFieldValue().equalsIgnoreCase((String) FieldUtils.readField(obj, field.getName(), true))){
                                    hasFacet = true;
                                    break;
                                }
                            }
                        }
                    }
                }

                result = result && hasFacet;
            }
        }
        catch (Exception e){
            String errorMessage = "An error occurred while determining if the job has the facet value. " + e.getLocalizedMessage();
            _logger.error(errorMessage, e);
        }

        return result;
    }
}
