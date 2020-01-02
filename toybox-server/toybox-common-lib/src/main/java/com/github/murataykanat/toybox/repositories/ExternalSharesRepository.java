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
    @Query(value = "SELECT id, username, creation_date, job_id, expiration_date, max_number_of_hits, notify_on_download, enable_expire, enable_usage_limit, url FROM external_shares WHERE id=?1", nativeQuery = true)
    List<ExternalShare> getExternalSharesById(String externalShareId);

    @Query(value = "SELECT id, username, creation_date, job_id, expiration_date, max_number_of_hits, notify_on_download, enable_expire, enable_usage_limit, url FROM external_shares WHERE username=?1", nativeQuery = true)
    List<ExternalShare> getExternalSharesByUsername(String username);

    @Query(value = "SELECT id, username, creation_date, job_id, expiration_date, max_number_of_hits, notify_on_download, enable_expire, enable_usage_limit, url FROM external_shares", nativeQuery = true)
    List<ExternalShare> getAllExternalShares();

    @Transactional
    @Modifying
    @Query(value = "INSERT INTO external_shares(id, username, creation_date, job_id, expiration_date, max_number_of_hits, notify_on_download, enable_expire, enable_usage_limit, url) VALUES (:id, :username, :creation_date, :job_id, :expiration_date, :max_number_of_hits, :notify_on_download, :enable_expire, :enable_usage_limit, :url)", nativeQuery = true)
    int insertExternalShare(@Param("id") String id, @Param("username") String username, @Param("job_id") long jobId,
                            @Param("creation_date") Date creationDate,
                            @Param("expiration_date") Date expirationDate, @Param("max_number_of_hits") int maxNumberOfHits,
                            @Param("notify_on_download") String notifyOnDownload, @Param("enable_expire") String enableExpire,
                            @Param("enable_usage_limit") String enableUsageLimit, @Param("url") String url);

    @Transactional
    @Modifying
    @Query(value = "UPDATE external_shares SET max_number_of_hits=?1 WHERE id=?2", nativeQuery = true)
    int updateMaxUsage(int maxNumberOfHits, String id);
}