package com.github.murataykanat.toybox.repositories;

import com.github.murataykanat.toybox.dbo.ContainerUser;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

public interface ContainerUsersRepository extends JpaRepository<ContainerUser, String> {
    @Transactional
    @Modifying
    @Query(value = "INSERT INTO container_user(container_id, user_id) VALUES (?1, ?2)", nativeQuery = true)
    int insertSubscriber(String containerId, int userId);

    @Transactional
    @Modifying
    @Query(value = "DELETE FROM container_user WHERE container_id=?1 AND user_id=?2", nativeQuery = true)
    int deleteSubscriber(String containerId, int userId);

    @Transactional
    @Modifying
    @Query(value = "DELETE FROM container_user WHERE container_id=?1", nativeQuery = true)
    int deleteAllSubscribersByContainerId(String containerId);

    @Query(value = "SELECT container_id, user_id FROM container_user WHERE user_id=?1", nativeQuery = true)
    List<ContainerUser> findContainerUsersByUserId(int userId);
    @Query(value = "SELECT container_id, user_id FROM container_user WHERE container_id=?1", nativeQuery = true)
    List<ContainerUser> findContainerUsersByContainerId(String containerId);
}
