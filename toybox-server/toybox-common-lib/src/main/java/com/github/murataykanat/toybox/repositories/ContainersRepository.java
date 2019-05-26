package com.github.murataykanat.toybox.repositories;

import com.github.murataykanat.toybox.dbo.Container;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.transaction.annotation.Transactional;

import java.util.Date;
import java.util.List;

public interface ContainersRepository extends JpaRepository<Container, String> {
    @Query(value = "SELECT container_id, container_name, container_created_by_username, container_creation_date, deleted FROM containers WHERE deleted='N'", nativeQuery = true)
    List<Container> getNonDeletedContainers();

    @Query(value = "SELECT container_id, container_name, container_created_by_username, container_creation_date, deleted FROM containers WHERE container_id=?1", nativeQuery = true)
    List<Container> getContainersById(String containerId);

    @Query(value = "SELECT container_id, container_name, container_created_by_username, container_creation_date, deleted FROM containers WHERE container_name=?1 AND container_created_by_username=?2", nativeQuery = true)
    List<Container> getDuplicateContainersByContainerNameAndUsername(String containerName, String username);

    @Transactional
    @Modifying
    @Query(value = "UPDATE containers SET container_name=?1 WHERE container_id=?3", nativeQuery = true)
    int updateContainerName(String containerName, String containerId);

    @Modifying
    @Query(value = "INSERT INTO containers(container_id, container_name, container_created_by_username, container_creation_date, deleted) " +
            "VALUES (:container_id, :container_name, :container_created_by_username, :container_creation_date, :deleted)", nativeQuery = true)
    int insertContainer(@Param("container_id") String containerId, @Param("container_name") String containerName,
                    @Param("container_created_by_username") String username, @Param("container_creation_date") Date creationDate, @Param("deleted") String deleted);

    @Transactional
    @Modifying
    @Query(value = "UPDATE containers SET deleted=:deleted WHERE asset_id IN :containerIds", nativeQuery = true)
    int deleteContainersById(@Param("deleted") String deleted, @Param("containerIds") List<String> containerIds);
}
