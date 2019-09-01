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
    @Query(value = "SELECT container_id, parent_container_id, container_name, container_created_by_username, container_creation_date, deleted, is_system FROM containers WHERE deleted='N' AND parent_container_id IS NULL", nativeQuery = true)
    List<Container> getTopLevelNonDeletedContainers();

    @Query(value = "SELECT container_id, parent_container_id, container_name, container_created_by_username, container_creation_date, deleted, is_system FROM containers WHERE deleted='N' AND container_created_by_username=?1 AND parent_container_id=?2", nativeQuery = true)
    List<Container> getNonDeletedContainersByUsernameAndParentContainerId(String username, String parentContainerId);

    @Query(value = "SELECT container_id, parent_container_id, container_name, container_created_by_username, container_creation_date, deleted, is_system FROM containers WHERE deleted='N'", nativeQuery = true)
    List<Container> getAllNonDeletedContainers();

    @Query(value = "SELECT container_id, parent_container_id, container_name, container_created_by_username, container_creation_date, deleted, is_system FROM containers WHERE deleted='N' AND parent_container_id=?1", nativeQuery = true)
    List<Container> getNonDeletedContainersByParentContainerId(String parentContainerId);

    @Query(value = "SELECT container_id, parent_container_id, container_name, container_created_by_username, container_creation_date, deleted, is_system FROM containers WHERE container_id=?1", nativeQuery = true)
    List<Container> getContainersById(String containerId);

    @Query(value = "SELECT container_id, parent_container_id, container_name, container_created_by_username, container_creation_date, deleted, is_system FROM containers WHERE parent_container_id=?1", nativeQuery = true)
    List<Container> getContainersByParentContainerId(String parentContainerId);

    @Query(value = "SELECT container_id, parent_container_id, container_name, container_created_by_username, container_creation_date, deleted, is_system FROM containers WHERE container_name=?1 AND is_system='Y'", nativeQuery = true)
    List<Container> getSystemContainersByName(String name);

    @Query(value = "SELECT container_id, parent_container_id, container_name, container_created_by_username, container_creation_date, deleted, is_system FROM containers WHERE (container_name=?1 AND is_system='Y') OR container_created_by_username=?1", nativeQuery = true)
    List<Container> getUserFolders(String username);

    @Transactional
    @Modifying
    @Query(value = "UPDATE containers SET container_name=?1 WHERE container_id=?2", nativeQuery = true)
    int updateContainerName(String containerName, String containerId);

    @Transactional
    @Modifying
    @Query(value = "UPDATE containers SET parent_container_id=?1 WHERE container_id=?2", nativeQuery = true)
    int updateContainerParentContainerId(String parentContainerId, String containerId);

    @Transactional
    @Modifying
    @Query(value = "INSERT INTO containers(container_id, parent_container_id, container_name, container_created_by_username, container_creation_date, deleted, is_system) " +
            "VALUES (:container_id, :parent_container_id, :container_name, :container_created_by_username, :container_creation_date, :deleted, :is_system)", nativeQuery = true)
    int insertContainer(@Param("container_id") String containerId, @Param("container_name") String containerName,
                        @Param("parent_container_id") String parentContainerId, @Param("container_created_by_username") String username,
                        @Param("container_creation_date") Date creationDate, @Param("deleted") String deleted, @Param("is_system") String system);

    @Transactional
    @Modifying
    @Query(value = "UPDATE containers SET deleted=:deleted WHERE container_id IN :containerIds", nativeQuery = true)
    int deleteContainersById(@Param("deleted") String deleted, @Param("containerIds") List<String> containerIds);
}
