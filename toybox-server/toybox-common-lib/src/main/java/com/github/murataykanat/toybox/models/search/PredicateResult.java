package com.github.murataykanat.toybox.models.search;

import com.querydsl.core.types.dsl.BooleanExpression;

public class PredicateResult {
    private BooleanExpression booleanExpression;
    private String operator;

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
}
