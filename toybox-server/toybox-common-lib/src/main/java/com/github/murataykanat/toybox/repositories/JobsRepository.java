package com.github.murataykanat.toybox.repositories;

import com.github.murataykanat.toybox.models.job.QToyboxJob;
import com.github.murataykanat.toybox.models.job.ToyboxJob;
import com.querydsl.core.types.dsl.StringExpression;
import com.querydsl.core.types.dsl.StringPath;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.querydsl.QueryDslPredicateExecutor;
import org.springframework.data.querydsl.binding.QuerydslBinderCustomizer;
import org.springframework.data.querydsl.binding.QuerydslBindings;
import org.springframework.data.querydsl.binding.SingleValueBinding;

import java.util.List;

public interface JobsRepository extends JpaRepository<ToyboxJob, String>, QueryDslPredicateExecutor<ToyboxJob>, QuerydslBinderCustomizer<QToyboxJob> {
    @Override
    default public void customize(QuerydslBindings bindings, QToyboxJob root) {
        bindings.bind(String.class)
                .first((SingleValueBinding<StringPath, String>) StringExpression::containsIgnoreCase);
    }

    @Query(value = "SELECT JOB_INSTANCE_ID, JOB_EXECUTION_ID, JOB_NAME, JOB_TYPE, START_TIME, END_TIME, STATUS, USERNAME  FROM TOYBOX_JOBS_VW WHERE JOB_INSTANCE_ID=?1", nativeQuery = true)
    List<ToyboxJob> getJobsByInstanceId(String instanceId);
}
