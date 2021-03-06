package com.github.murataykanat.toybox.predicates;

import com.github.murataykanat.toybox.contants.ToyboxConstants;
import com.github.murataykanat.toybox.schema.search.SearchCondition;
import com.querydsl.core.types.dsl.*;
import org.apache.commons.lang.WordUtils;

import java.util.Calendar;
import java.util.Date;

public class ToyboxPredicate<T> {
    private SearchCondition searchCondition;
    private Class<T> typeParameterClass;

    public ToyboxPredicate(SearchCondition searchCondition, Class<T> typeParameterClass){
        this.searchCondition = searchCondition;
        this.typeParameterClass = typeParameterClass;
    }

    public BooleanExpression getPredicate() {

        PathBuilder<T> entityPath = new PathBuilder<>(typeParameterClass, WordUtils.uncapitalize(typeParameterClass.getSimpleName()));

        String field = searchCondition.getField();
        String keyword = searchCondition.getKeyword();
        String operator = searchCondition.getOperator();
        String dataType = searchCondition.getDataType();

        if (ToyboxConstants.SEARCH_CONDITION_DATA_TYPE_INTEGER.equalsIgnoreCase(dataType)) {
            NumberPath<Integer> path = entityPath.getNumber(field, Integer.class);
            int value = Integer.parseInt(keyword);
            switch (operator) {
                case ToyboxConstants.SEARCH_CONDITION_EQUALS:
                    return path.eq(value);
                case ToyboxConstants.SEARCH_CONDITION_IS_GREATER_THAN:
                    return path.goe(value);
                case ToyboxConstants.SEARCH_CONDITION_IS_LESS_THAN:
                    return path.loe(value);
            }
        }
        else if(ToyboxConstants.SEARCH_CONDITION_DATA_TYPE_DATE.equalsIgnoreCase(dataType)){
            DatePath<Date> path = entityPath.getDate(field, Date.class);

            Calendar cal = Calendar.getInstance();
            cal.set(Calendar.HOUR_OF_DAY, 0);
            cal.set(Calendar.MINUTE, 0);
            cal.set(Calendar.SECOND, 0);
            Date today = cal.getTime();

            cal = Calendar.getInstance();
            cal.set(Calendar.HOUR_OF_DAY, 0);
            cal.set(Calendar.MINUTE, 0);
            cal.set(Calendar.SECOND, 0);
            cal.add(Calendar.DAY_OF_MONTH, 1);
            Date tomorrow = cal.getTime();

            cal = Calendar.getInstance();
            cal.set(Calendar.HOUR_OF_DAY, 0);
            cal.set(Calendar.MINUTE, 0);
            cal.set(Calendar.SECOND, 0);
            cal.add(Calendar.DAY_OF_MONTH, -7);
            Date sevenDaysAgo = cal.getTime();

            cal = Calendar.getInstance();
            cal.set(Calendar.HOUR_OF_DAY, 0);
            cal.set(Calendar.MINUTE, 0);
            cal.set(Calendar.SECOND, 0);
            cal.add(Calendar.DAY_OF_MONTH, 7);
            Date sevenDaysLater = cal.getTime();

            cal = Calendar.getInstance();
            cal.set(Calendar.HOUR_OF_DAY, 0);
            cal.set(Calendar.MINUTE, 0);
            cal.set(Calendar.SECOND, 0);
            cal.add(Calendar.DAY_OF_MONTH, -30);
            Date thirtyDaysAgo = cal.getTime();

            cal = Calendar.getInstance();
            cal.set(Calendar.HOUR_OF_DAY, 0);
            cal.set(Calendar.MINUTE, 0);
            cal.set(Calendar.SECOND, 0);
            cal.add(Calendar.DAY_OF_MONTH, 30);
            Date thirtyDaysLater = cal.getTime();


            switch (keyword){
                case ToyboxConstants.LOOKUP_NEXT_30_PLUS_DAYS:
                    return path.after(thirtyDaysLater);
                case ToyboxConstants.LOOKUP_NEXT_30_DAYS:
                    return path.between(today, thirtyDaysLater);
                case ToyboxConstants.LOOKUP_NEXT_7_DAYS:
                    return path.between(today, sevenDaysLater);
                case ToyboxConstants.LOOKUP_TODAY:
                    return path.between(today, tomorrow);
                case ToyboxConstants.LOOKUP_PAST_7_DAYS:
                    return path.between(today, sevenDaysAgo);
                case ToyboxConstants.LOOKUP_PAST_30_DAYS:
                    return path.between(today, thirtyDaysAgo);
                case ToyboxConstants.LOOKUP_PAST_30_PLUS_DAYS:
                    return path.before(thirtyDaysAgo);
            }
        }
        else if(ToyboxConstants.SEARCH_CONDITION_DATA_TYPE_STRING.equalsIgnoreCase(dataType)){
            StringPath path = entityPath.getString(field);
            switch (operator) {
                case ToyboxConstants.SEARCH_CONDITION_IS_NULL:
                    return path.isNull();
                case ToyboxConstants.SEARCH_CONDITION_IS_NOT_NULL:
                    return path.isNotNull();
                case ToyboxConstants.SEARCH_CONDITION_EQUALS:
                    return path.equalsIgnoreCase(keyword);
                case ToyboxConstants.SEARCH_CONDITION_CONTAINS:
                    return path.containsIgnoreCase(keyword);
                case ToyboxConstants.SEARCH_CONDITION_STARTS_WITH:
                    return path.startsWithIgnoreCase(keyword);
                case ToyboxConstants.SEARCH_CONDITION_ENDS_WITH:
                    return path.endsWithIgnoreCase(keyword);
            }
        }
        else{
            throw new IllegalArgumentException("Data type '" + dataType + "' is not recognized!");
        }

        return null;
    }
}
