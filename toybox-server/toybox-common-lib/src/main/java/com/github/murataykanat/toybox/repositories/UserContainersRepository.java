package com.github.murataykanat.toybox.repositories;

import com.github.murataykanat.toybox.dbo.UserContainer;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

public interface UserContainersRepository extends JpaRepository<UserContainer, Integer> {
    @Query(value = "SELECT container_id, user_id FROM user_container WHERE user_id=?1 AND container_id=?2", nativeQuery = true)
    List<UserContainer> findUserAssetsByUserIdAndContainerId(int userId, String containerId);
    @Transactional
    @Modifying
    @Query(value = "INSERT INTO user_container(container_id, user_id) VALUES (?1, ?2)", nativeQuery = true)
    int insertSharedContainer(String containerId, int userId);
    @Transactional
    @Modifying
    @Query(value = "DELETE FROM user_container WHERE container_id=?1 AND user_id=?2", nativeQuery = true)
    int deleteSharedAsset(String containerId, int userId);
}
