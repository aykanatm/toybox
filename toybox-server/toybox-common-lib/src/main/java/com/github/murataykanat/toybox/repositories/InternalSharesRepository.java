package com.github.murataykanat.toybox.repositories;

import com.github.murataykanat.toybox.dbo.InternalShare;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.transaction.annotation.Transactional;

import java.util.Date;
import java.util.List;

public interface InternalSharesRepository extends JpaRepository<InternalShare, String> {
    @Query(value = "SELECT id, username, creation_date, enable_expire, expiration_date, notify_on_edit, notify_on_download, notify_on_share, notify_on_copy, can_edit, can_download, can_share, can_copy FROM internal_shares WHERE id=?1", nativeQuery = true)
    List<InternalShare> getInternalSharesById(String id);

    @Query(value = "SELECT id, username, creation_date, enable_expire, expiration_date, notify_on_edit, notify_on_download, notify_on_share, notify_on_copy, can_edit, can_download, can_share, can_copy FROM internal_shares WHERE id IN :internalShareIds", nativeQuery = true)
    List<InternalShare> getInternalSharesByIds(@Param("internalShareIds")List<String> internalShareIds);

    @Query(value = "SELECT id, username, creation_date, enable_expire, expiration_date, notify_on_edit, notify_on_download, notify_on_share, notify_on_copy, can_edit, can_download, can_share, can_copy FROM internal_shares WHERE username=?1", nativeQuery = true)
    List<InternalShare> getInternalSharesByUsername(String username);

    @Query(value = "SELECT id, username, creation_date, enable_expire, expiration_date, notify_on_edit, notify_on_download, notify_on_share, notify_on_copy, can_edit, can_download, can_share, can_copy FROM internal_shares", nativeQuery = true)
    List<InternalShare> getAllInternalShares();

    @Transactional
    @Modifying
    @Query(value = "UPDATE internal_shares SET enable_expire=?2, expiration_date=?3, notify_on_edit=?4, notify_on_download=?5, notify_on_share=?6, notify_on_copy=?7, can_edit=?8, can_download=?9, can_share=?10, can_copy=?11 WHERE id=?1", nativeQuery = true)
    int updateInternalShareById(String id, String enableExpire, Date expirationDate, String notifyOnEdit, String notifyOnDownload, String notifyOnShare,
                        String notifyOnCopy, String canEdit, String canDownload, String canShare, String canCopy);

    @Transactional
    @Modifying
    @Query(value = "INSERT INTO internal_shares(id, username, creation_date, enable_expire, expiration_date, notify_on_edit, notify_on_download, notify_on_share, notify_on_copy, can_edit, can_download, can_share, can_copy) VALUES (:id, :username, :creation_date, :enable_expire, :expiration_date, :notify_on_edit, :notify_on_download, :notify_on_share, :notify_on_copy, :can_edit, :can_download, :can_share, :can_copy)", nativeQuery = true)
    int insertInternalShare(@Param("id") String id, @Param("username") String username, @Param("creation_date") Date creationDate,
                            @Param("enable_expire") String enableExpire,
                            @Param("expiration_date") Date expirationDate, @Param("notify_on_edit") String notifyOnEdit, @Param("notify_on_download") String notifyOnDownload,
                            @Param("notify_on_share") String notifyOnShare, @Param("notify_on_copy") String notifyOnCopy, @Param("can_edit") String canEdit,
                            @Param("can_download") String canDownload, @Param("can_share") String canShare, @Param("can_copy") String canCopy);
}
