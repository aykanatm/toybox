package com.github.murataykanat.toybox.repositories;

import com.github.murataykanat.toybox.dbo.ExternalShare;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.transaction.annotation.Transactional;

import java.util.Date;
import java.util.List;

public interface ExternalSharesRepository extends JpaRepository<ExternalShare, String> {
    @Query(value = "SELECT external_share_id, username, creation_date, job_id, expiration_date, max_number_of_hits, notify_when_downloaded, enable_expire_external, enable_usage_limit FROM external_shares WHERE external_share_id=?1", nativeQuery = true)
    List<ExternalShare> getExternalSharesById(String externalShareId);

    @Transactional
    @Modifying
    @Query(value = "INSERT INTO external_shares(external_share_id, username, creation_date, job_id, expiration_date, max_number_of_hits, notify_when_downloaded, enable_expire_external, enable_usage_limit) VALUES (:external_share_id, :username, :creation_date, :job_id, :expiration_date, :max_number_of_hits, :notify_when_downloaded, :enable_expire_external, :enable_usage_limit)", nativeQuery = true)
    int insertExternalShare(@Param("external_share_id") String externalShareId, @Param("username") String username, @Param("job_id") long jobId,
                            @Param("creation_date") Date creationDate,
                            @Param("expiration_date") Date expirationDate, @Param("max_number_of_hits") int maxNumberOfHits,
                            @Param("notify_when_downloaded") String notifyWhenDownloaded, @Param("enable_expire_external") String enableExpireExternal,
                            @Param("enable_usage_limit") String enableUsageLimit);

    @Transactional
    @Modifying
    @Query(value = "UPDATE external_shares SET max_number_of_hits=?1 WHERE external_share_id=?2", nativeQuery = true)
    int updateMaxUsage(int maxNumberOfHits, String externalShareId);
}