package com.github.murataykanat.toybox.repositories;

import com.github.murataykanat.toybox.dbo.InternalShareUser;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

public interface InternalShareUsersRepository extends JpaRepository<InternalShareUser, Integer> {
    @Query(value = "SELECT internal_share_id, user_id FROM internal_share_users WHERE internal_share_id=?1 AND user_id=?2", nativeQuery = true)
    List<InternalShareUser> findInternalShareUsersByInternalShareIdAndUserId(String internalShareId, int userId);

    @Query(value = "SELECT internal_share_id, user_id FROM internal_share_users WHERE user_id=?1", nativeQuery = true)
    List<InternalShareUser> findInternalShareUsersByUserId(int userId);

    @Query(value = "SELECT internal_share_id, user_id FROM internal_share_users WHERE internal_share_id=?1", nativeQuery = true)
    List<InternalShareUser> findInternalShareUsersByInternalShareId(String internalShareId);

    @Transactional
    @Modifying
    @Query(value = "INSERT INTO internal_share_users(internal_share_id, user_id) VALUES (?1, ?2)", nativeQuery = true)
    int insertShareUser(String internalShareId, int userId);

    @Transactional
    @Modifying
    @Query(value = "DELETE FROM internal_share_users WHERE internal_share_id=?1 AND user_id=?2", nativeQuery = true)
    int deleteShareUserByInternalShareIdAndUserId(String internalShareId, int userId);

    @Transactional
    @Modifying
    @Query(value = "DELETE FROM internal_share_users WHERE internal_share_id=?1", nativeQuery = true)
    int deleteShareUsersByInternalShareId(String internalShareId);
}
