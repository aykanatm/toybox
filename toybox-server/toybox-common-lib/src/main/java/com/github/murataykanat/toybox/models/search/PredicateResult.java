package com.github.murataykanat.toybox.models.search;

import com.querydsl.core.types.dsl.BooleanExpression;

public class PredicateResult {
    private BooleanExpression booleanExpression;
    private String operator;
    private PredicateResult innerPredicateResult;

    public BooleanExpression getBooleanExpression() {
        return booleanExpression;
    }

    public void setBooleanExpression(BooleanExpression booleanExpression) {
        this.booleanExpression = booleanExpression;
    }

    public String getOperator() {
        return operator;
    }

    public void setOperator(String operator) {
        this.operator = operator;
    }

    public PredicateResult getInnerPredicateResult() {
        return innerPredicateResult;
    }

    public void setInnerPredicateResult(PredicateResult innerPredicateResult) {
        this.innerPredicateResult = innerPredicateResult;
    }
}
