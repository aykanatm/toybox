package com.github.murataykanat.toybox.utilities;

import com.github.murataykanat.toybox.annotations.LogEntryExitExecutionTime;
import com.github.murataykanat.toybox.contants.ToyboxConstants;
import com.github.murataykanat.toybox.models.annotations.FacetColumnName;
import com.github.murataykanat.toybox.models.annotations.FacetDataType;
import com.github.murataykanat.toybox.models.annotations.FacetDefaultLookup;
import com.github.murataykanat.toybox.models.search.FacetField;
import com.github.murataykanat.toybox.schema.common.Facet;
import org.apache.commons.lang.reflect.FieldUtils;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.springframework.stereotype.Component;

import javax.persistence.Column;
import java.lang.reflect.Field;
import java.util.*;
import java.util.function.Function;
import java.util.stream.Collectors;

@Component
public class FacetUtils {
    private static final Log _logger = LogFactory.getLog(FacetUtils.class);

    @LogEntryExitExecutionTime
    public <T> FacetField getFacetField(String fieldName, T object){
        List<Field> declaredFields = Arrays.asList(object.getClass().getDeclaredFields());
        for(Field field: declaredFields){
            if(field.getAnnotation(Column.class) != null && field.getAnnotation(FacetColumnName.class) != null){
                if(field.getAnnotation(FacetColumnName.class).value().equalsIgnoreCase(fieldName)){
                    // The reason why we are using the actual field name is because
                    // hibernate cannot match the column name with the variable name
                    FacetField facetField = new FacetField();
                    facetField.setFieldName(field.getName());

                    if(ToyboxConstants.SEARCH_CONDITION_DATA_TYPE_STRING.equalsIgnoreCase(field.getType().getSimpleName())){
                        facetField.setDataType(ToyboxConstants.SEARCH_CONDITION_DATA_TYPE_STRING);
                    }
                    else if(ToyboxConstants.SEARCH_CONDITION_DATA_TYPE_INTEGER.equalsIgnoreCase(field.getType().getSimpleName())){
                        facetField.setDataType(ToyboxConstants.SEARCH_CONDITION_DATA_TYPE_INTEGER);
                    }
                    else if(ToyboxConstants.SEARCH_CONDITION_DATA_TYPE_DATE.equalsIgnoreCase(field.getType().getSimpleName())){
                        facetField.setDataType(ToyboxConstants.SEARCH_CONDITION_DATA_TYPE_DATE);
                    }
                    else{
                        throw new IllegalArgumentException("Field type '" + field.getType().getSimpleName() + "' is invalid!");
                    }

                    return facetField;
                }
            }
        }

        throw new IllegalArgumentException("Field name '" + fieldName + "' does not exist in the object of class '" + object.getClass() + "'!");
    }

    @LogEntryExitExecutionTime
    public <T> List<Facet>  getFacets (List<T> objects) throws IllegalAccessException {
        List<Facet> facets = new ArrayList<>();
        if(!objects.isEmpty()){
            HashSet<String> uniqueClasses = objects.stream().map(o -> o.getClass().getName()).collect(Collectors.toCollection(HashSet::new));
            List<T> uniqueObjects = new ArrayList<>();
            for(String uniqueClass: uniqueClasses){
                uniqueObjects.add(objects.stream().filter(o -> o.getClass().getName().equalsIgnoreCase(uniqueClass)).findAny().get());
            }

            List<Field> allFields = uniqueObjects.stream()
                    .map(o -> o.getClass().getDeclaredFields())
                    .flatMap(Arrays::stream)
                    .collect(Collectors.toList());

            List<Field> declaredFields;
            if(uniqueObjects.size() > 1){
                Set<Field> duplicateFields = findDuplicates(allFields, Field::getName);
                declaredFields = new ArrayList<>(duplicateFields);
            }
            else{
                declaredFields = allFields;
            }

            for(Field field: declaredFields){
                if(field.isAnnotationPresent(FacetColumnName.class)){
                    _logger.debug("Field name: " + field.getName());
                    Facet facet = new Facet();

                    String facetFieldName = field.getAnnotation(FacetColumnName.class).value();
                    facet.setName(facetFieldName);
                    if(field.getAnnotation(Column.class) != null){
                        String facetDbFieldName = field.getAnnotation(Column.class).name();
                        facet.setDbFieldName(facetDbFieldName);
                    }

                    List<String> lookups = new ArrayList<>();

                    for(T obj: objects){
                        for(Field objField: obj.getClass().getDeclaredFields()){
                            if(objField.isAnnotationPresent(FacetColumnName.class) && objField.getAnnotation(FacetColumnName.class).value().equalsIgnoreCase(facetFieldName)){
                                if(objField.isAnnotationPresent(FacetDefaultLookup.class) && objField.getAnnotation(FacetDataType.class).value().equalsIgnoreCase("Date")){
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

                                    Date dateValue = (Date) FieldUtils.readField(obj, field.getName(), true);
                                    if(dateValue != null){
                                        if((dateValue.after(today) || dateValue.equals(today)) && dateValue.before(tomorrow)){
                                            lookups.add("Today");
                                        }
                                        else if(dateValue.before(sevenDaysLater) && dateValue.after(today)){
                                            lookups.add("Next 7 days");
                                        }
                                        else if(dateValue.after(sevenDaysAgo) && dateValue.before(tomorrow)){
                                            lookups.add("Past 7 days");
                                        }
                                        else if(dateValue.after(today) && dateValue.before(thirtyDaysLater)){
                                            lookups.add("Next 30 days");
                                        }
                                        else if(dateValue.after(thirtyDaysAgo) && dateValue.before(tomorrow)){
                                            lookups.add("Past 30 days");
                                        }
                                        else if(dateValue.after(thirtyDaysLater)){
                                            lookups.add("Next 30+ days");
                                        }
                                        else if(dateValue.before(thirtyDaysAgo)){
                                            lookups.add("Past 30+ days");
                                        }
                                    }
                                }
                                else{
                                    lookups.add((String) FieldUtils.readField(obj, field.getName(), true));
                                }

                                break;
                            }
                        }
                    }

                    lookups = new ArrayList<>(new HashSet<>(lookups));
                    facet.setLookups(lookups);

                    facets.add(facet);
                }
            }
        }

        return facets;
    }

    @LogEntryExitExecutionTime
    private <U, T> Set<T> findDuplicates(Collection<T> collection, Function<? super T,? extends U> keyExtractor) {
        Map<U, T> uniques = new HashMap<>(); // maps unique keys to corresponding values
        return collection.stream()
                .filter(e -> uniques.put(keyExtractor.apply(e), e) != null)
                .collect(Collectors.toSet());
    }

    @LogEntryExitExecutionTime
    public List<Facet> getCommonFacets(List<Facet> facetList1, List<Facet> facetList2){
        List<Facet> result = new ArrayList<>();
        List<Facet> allFacets = new ArrayList<>();

        allFacets.addAll(facetList1);
        allFacets.addAll(facetList2);

        for(Facet facet: allFacets){
            List<Facet> facetsWithName = allFacets.stream().filter(f -> f.getName().equalsIgnoreCase(facet.getName())).collect(Collectors.toList());
            if(!facetsWithName.isEmpty()){
                boolean isInResultsAlready = result.stream().anyMatch(f -> f.getName().equalsIgnoreCase(facet.getName()));
                if(!isInResultsAlready){
                    if(facetsWithName.size() > 1){
                        facetsWithName.stream().max(Comparator.comparing(f -> f.getLookups().size(), Comparator.nullsLast(Comparator.naturalOrder()))).ifPresent(result::add);
                    }
                    else{
                        result.add(facet);
                    }
                }
            }
        }

        return result;
    }
}