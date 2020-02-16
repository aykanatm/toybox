package com.github.murataykanat.toybox.predicates;

import com.querydsl.core.types.Path;
import com.querydsl.core.types.dsl.StringPath;

public class ToyboxStringPath extends StringPath {
    public ToyboxStringPath(Path<?> parent, String property) {
        super(parent, property);
    }
}
