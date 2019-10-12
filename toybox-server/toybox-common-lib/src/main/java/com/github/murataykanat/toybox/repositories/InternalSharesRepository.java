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
    @Query(value = "SELECT internal_share_id, username, expiration_date, notify_on_view, notify_on_edit, notify_on_download, notify_on_share, notify_on_move_or_copy, can_view, can_edit, can_download, can_share, can_move_or_copy FROM internal_shares WHERE internal_share_id=?1", nativeQuery = true)
    List<InternalShare> getInternalSharesById(String internalShareId);

    @Transactional
    @Modifying
    @Query(value = "INSERT INTO internal_shares(internal_share_id, username, expiration_date, notify_on_view, notify_on_edit, notify_on_download, notify_on_share, notify_on_move_or_copy, can_view, can_edit, can_download, can_share, can_move_or_copy) VALUES (:internal_share_id, :username, :expiration_date, :notify_on_view, :notify_on_edit, :notify_on_download, :notify_on_share, :notify_on_move_or_copy, :can_view, :can_edit, :can_download, :can_share, :can_move_or_copy)", nativeQuery = true)
    int insertExternalShare(@Param("internal_share_id") String internalShareId, @Param("username") String username, @Param("expiration_date") Date expirationDate,
                            @Param("notify_on_view") String notifyOnView, @Param("notify_on_edit") String notifyOnEdit,
                            @Param("notify_on_download") String notifyOnDownload, @Param("notify_on_share") String notifyOnShare,
                            @Param("notify_on_move_or_copy") String notifyOnMoveOrCopy, @Param("can_view") String canView,
                            @Param("can_edit") String canEdit, @Param("can_download") String canDownload, @Param("can_share") String canShare,
                            @Param("can_move_or_copy") String canMoveOrCopy);
}