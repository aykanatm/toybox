package com.github.murataykanat.toybox.predicates;

import com.github.murataykanat.toybox.contants.ToyboxConstants;
import com.github.murataykanat.toybox.models.search.PredicateResult;
import com.github.murataykanat.toybox.schema.search.SearchCondition;
import com.querydsl.core.types.dsl.BooleanExpression;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

public class ToyboxPredicateBuilder<T> {
    private List<SearchCondition> searchConditions;
    private Class<T> typeParameterClass;

    public ToyboxPredicateBuilder<T> with(List<SearchCondition> searchConditions, Class<T> typeParameterClass) {
        this.searchConditions = searchConditions;
        this.typeParameterClass = typeParameterClass;
        return this;
    }

    public BooleanExpression build() {
        if (searchConditions.isEmpty()) {
            return null;
        }

        List<PredicateResult> predicateResults = searchConditions.stream()
                .map(param -> {
                    ToyboxPredicate<T> predicate = new ToyboxPredicate<>(param, typeParameterClass);
                    PredicateResult predicateResult = new PredicateResult();
                    predicateResult.setBooleanExpression(predicate.getPredicate());
                    predicateResult.setOperator(param.getBooleanOperator());
                    return predicateResult;
                }).collect(Collectors.toList());

        List<PredicateResult> orInPredicates = new ArrayList<>();
        List<PredicateResult> andInPredicates = new ArrayList<>();

        BooleanExpression result = null;
        for (PredicateResult predicateResult : predicateResults) {
            if(predicateResult.getOperator().equalsIgnoreCase(ToyboxConstants.SEARCH_OPERATOR_AND)){
                if(result != null){
                    result = result.and(predicateResult.getBooleanExpression());
                }
                else{
                    result = predicateResult.getBooleanExpression();
                }
            }
            else if(predicateResult.getOperator().equalsIgnoreCase(ToyboxConstants.SEARCH_OPERATOR_OR)){
                if(result != null){
                    result = result.or(predicateResult.getBooleanExpression());
                }
                else{
                    result = predicateResult.getBooleanExpression();
                }
            }
            else if(predicateResult.getOperator().equalsIgnoreCase(ToyboxConstants.SEARCH_OPERATOR_OR_IN)){
                orInPredicates.add(predicateResult);
            }
            else if(predicateResult.getOperator().equalsIgnoreCase(ToyboxConstants.SEARCH_OPERATOR_AND_IN)){
                andInPredicates.add(predicateResult);
            }
            else{
                throw new IllegalArgumentException("Boolean operator '" + predicateResult.getOperator() + "' is not recognized!");
            }
        }

        if(!andInPredicates.isEmpty()){
            BooleanExpression inResult = null;
            for(PredicateResult inPredicate: andInPredicates){
                if(inResult != null){
                    inResult = inResult.or(inPredicate.getBooleanExpression());
                }
                else{
                    inResult = inPredicate.getBooleanExpression();
                }
            }

            if(inResult != null){
                result = result.and(inResult);
            }
        }

        if(!orInPredicates.isEmpty()){
            BooleanExpression inResult = null;
            for(PredicateResult inPredicate: orInPredicates){
                if(inResult != null){
                    inResult = inResult.or(inPredicate.getBooleanExpression());
                }
                else{
                    inResult = inPredicate.getBooleanExpression();
                }
            }

            if(inResult != null){
                result = result.or(inResult);
            }
        }

        return result;
    }
}