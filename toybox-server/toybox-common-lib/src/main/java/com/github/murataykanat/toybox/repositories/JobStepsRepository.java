package com.github.murataykanat.toybox.repositories;

import com.github.murataykanat.toybox.models.job.ToyboxJobStep;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.util.List;

public interface JobStepsRepository extends JpaRepository<ToyboxJobStep, String> {
    @Query(value = "SELECT JOB_EXECUTION_ID, STEP_EXECUTION_ID, STEP_NAME, START_TIME, END_TIME, STATUS  FROM BATCH_STEP_EXECUTION WHERE JOB_EXECUTION_ID=?1", nativeQuery = true)
    List<ToyboxJobStep> getJobStepsByJobExecutionId(String jobExecutionId);
}
