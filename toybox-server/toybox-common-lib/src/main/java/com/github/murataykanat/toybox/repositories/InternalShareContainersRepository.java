package com.github.murataykanat.toybox.repositories;

import com.github.murataykanat.toybox.dbo.InternalShareContainer;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

public interface InternalShareContainersRepository extends JpaRepository<InternalShareContainer, String> {
    @Query(value = "SELECT internal_share_id, container_id FROM internal_share_containers WHERE internal_share_id=?1 AND container_id=?2", nativeQuery = true)
    List<InternalShareContainer> findInternalShareContainersByInternalShareIdAndContainerId(String internalShareId, String containerId);

    @Query(value = "SELECT internal_share_id, container_id FROM internal_share_containers WHERE container_id=?1", nativeQuery = true)
    List<InternalShareContainer> findInternalShareContainersByContainerId(String containerId);

    @Query(value = "SELECT internal_share_id, container_id FROM internal_share_containers WHERE internal_share_id=?1", nativeQuery = true)
    List<InternalShareContainer> findInternalShareContainersByInternalShareId(String internalShareId);

    @Transactional
    @Modifying
    @Query(value = "INSERT INTO internal_share_containers(internal_share_id, container_id) VALUES (?1, ?2)", nativeQuery = true)
    int insertSharedContainer(String internalShareId, String containerId);

    @Transactional
    @Modifying
    @Query(value = "DELETE FROM internal_share_containers WHERE internal_share_id=?1", nativeQuery = true)
    int deleteSharedContainerByInternalShareId(String internalShareId);

    @Transactional
    @Modifying
    @Query(value = "DELETE FROM internal_share_containers WHERE internal_share_id=?1 AND container_id=?2", nativeQuery = true)
    int deleteSharedContainerByInternalShareIdAndContainerId(String internalShareId, String containerId);
}
