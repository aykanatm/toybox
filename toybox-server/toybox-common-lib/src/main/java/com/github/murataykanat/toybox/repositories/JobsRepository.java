package com.github.murataykanat.toybox.repositories;

import com.github.murataykanat.toybox.models.job.ToyboxJob;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.util.List;

public interface JobsRepository extends JpaRepository<ToyboxJob, String> {
    @Query(value = "SELECT JOB_INSTANCE_ID, JOB_EXECUTION_ID, JOB_NAME, JOB_TYPE, START_TIME, END_TIME, STATUS, USERNAME  FROM TOYBOX_JOBS_VW", nativeQuery = true)
    List<ToyboxJob> getAll();
    @Query(value = "SELECT JOB_INSTANCE_ID, JOB_EXECUTION_ID, JOB_NAME, JOB_TYPE, START_TIME, END_TIME, STATUS, USERNAME  FROM TOYBOX_JOBS_VW WHERE JOB_INSTANCE_ID=?1", nativeQuery = true)
    List<ToyboxJob> getJobsByInstanceId(String instanceId);
}
