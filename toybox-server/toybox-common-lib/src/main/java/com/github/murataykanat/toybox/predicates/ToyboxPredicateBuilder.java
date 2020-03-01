package com.github.murataykanat.toybox.predicates;

import com.github.murataykanat.toybox.contants.ToyboxConstants;
import com.github.murataykanat.toybox.models.search.PredicateResult;
import com.github.murataykanat.toybox.schema.search.SearchCondition;
import com.querydsl.core.BooleanBuilder;
import com.querydsl.core.QueryMetadata;
import com.querydsl.core.types.Expression;
import com.querydsl.core.types.ExpressionUtils;
import com.querydsl.core.types.SubQueryExpression;
import com.querydsl.core.types.Visitor;
import com.querydsl.core.types.dsl.BooleanExpression;
import com.querydsl.core.types.dsl.Expressions;

import javax.annotation.Nullable;
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

        List<PredicateResult> inPredicates = new ArrayList<>();
        BooleanExpression result = Expressions.asBoolean(true).isTrue();
        for (PredicateResult predicateResult : predicateResults) {
            if(predicateResult.getOperator().equalsIgnoreCase(ToyboxConstants.SEARCH_OPERATOR_AND)){
                result = result.and(predicateResult.getBooleanExpression());
            }
            else if(predicateResult.getOperator().equalsIgnoreCase(ToyboxConstants.SEARCH_OPERATOR_OR)){
                result = result.or(predicateResult.getBooleanExpression());
            }
            else if(predicateResult.getOperator().equalsIgnoreCase(ToyboxConstants.SEARCH_OPERATOR_IN)){
                inPredicates.add(predicateResult);
            }
            else{
                throw new IllegalArgumentException("Boolean operator '" + predicateResult.getOperator() + "' is not recognized!");
            }
        }

        if(!inPredicates.isEmpty()){
            BooleanBuilder booleanBuilder = new BooleanBuilder();
            for(PredicateResult inPredicate: inPredicates){
                booleanBuilder.or(inPredicate.getBooleanExpression());
            }

            result = result.and(booleanBuilder);
        }

        return result;
    }
}