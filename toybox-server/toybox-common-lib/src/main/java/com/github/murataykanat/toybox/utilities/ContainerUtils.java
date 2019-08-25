package com.github.murataykanat.toybox.utilities;

import com.github.murataykanat.toybox.annotations.LogEntryExitExecutionTime;
import com.github.murataykanat.toybox.dbo.*;
import com.github.murataykanat.toybox.repositories.AssetsRepository;
import com.github.murataykanat.toybox.repositories.ContainerAssetsRepository;
import com.github.murataykanat.toybox.repositories.ContainerUsersRepository;
import com.github.murataykanat.toybox.repositories.ContainersRepository;
import com.github.murataykanat.toybox.schema.container.CreateContainerRequest;
import com.github.murataykanat.toybox.schema.container.CreateContainerResponse;
import com.github.murataykanat.toybox.schema.container.UpdateContainerRequest;
import org.apache.commons.lang.StringUtils;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.springframework.cloud.client.discovery.DiscoveryClient;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseEntity;
import org.springframework.web.client.RestTemplate;

import javax.servlet.http.HttpSession;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

public class ContainerUtils {
    private static final Log _logger = LogFactory.getLog(ContainerUtils.class);
    private static ContainerUtils containerUtils;

    private ContainerUtils(){}

    public static ContainerUtils getInstance(){
        if(containerUtils != null){
            return containerUtils;
        }

        containerUtils = new ContainerUtils();
        return containerUtils;
    }

    @LogEntryExitExecutionTime
    public Container getContainer(ContainersRepository containersRepository, String containerId) {
        List<Container> containersById = containersRepository.getContainersById(containerId);
        if(!containersById.isEmpty()){
            if(containersById.size() == 1){
                return containersById.get(0);
            }
            else{
                throw new IllegalArgumentException("There are multiple containers with ID '" + containerId + "'!");
            }
        }
        else{
            throw new IllegalArgumentException("There is no container with ID '" + containerId + "'!");
        }
    }

    @LogEntryExitExecutionTime
    public Container getUserContainer(ContainersRepository containersRepository, String username) {
        List<Container> systemContainersByName = containersRepository.getSystemContainersByName(username);
        if(!systemContainersByName.isEmpty()){
            if(systemContainersByName.size() == 1){
                return systemContainersByName.get(0);
            }
            else{
                throw new IllegalArgumentException("There are multiple system containers for the user '" + username + "'!");
            }
        }
        else{
            throw new IllegalArgumentException("There is no user container for the user '" + username + "'!");
        }
    }

    @LogEntryExitExecutionTime
    public boolean isSubscribed(ContainerUsersRepository containerUsersRepository, User user, Container asset){
        List<ContainerUser> containerUsersByUserId = containerUsersRepository.findContainerUsersByUserId(user.getId());
        for(ContainerUser containerUser: containerUsersByUserId){
            if(containerUser.getContainerId().equalsIgnoreCase(asset.getId())){
                return true;
            }
        }

        return false;
    }

    @LogEntryExitExecutionTime
    public int moveContainers(ContainersRepository containersRepository, ContainerAssetsRepository containerAssetsRepository, AssetsRepository assetsRepository, List<String> containerIds, Container targetContainer) throws Exception {
        int numberOfIgnoredContainers = 0;

        for(String containerId: containerIds){
            Container container = ContainerUtils.getInstance().getContainer(containersRepository, containerId);
            if(!container.getId().equalsIgnoreCase(targetContainer.getId())){
                if(!isSubfolder(containersRepository, targetContainer, container)){
                    Container duplicateContainer = findDuplicateContainer(containersRepository, targetContainer.getId(), container.getName());
                    if(duplicateContainer != null){
                        // Container has a duplicate inside the target folder
                        // We delete the container and move the contents to the duplicate container
                        List<String> assetIdsInsideContainer = containerAssetsRepository.findContainerAssetsByContainerId(container.getId()).stream().map(ContainerAsset::getAssetId).collect(Collectors.toList());
                        List<String> getNonDeletedLastVersionAssetIds = assetsRepository.getNonDeletedLastVersionAssetsByAssetIds(assetIdsInsideContainer).stream().map(Asset::getId).collect(Collectors.toList());
                        AssetUtils.getInstance().moveAssets(containerAssetsRepository, assetsRepository, getNonDeletedLastVersionAssetIds, duplicateContainer);

                        List<String> containerIdsToDelete = new ArrayList<>();
                        containerIdsToDelete.add(container.getId());

                        if(!targetContainer.getId().equalsIgnoreCase(container.getParentId())){
                            containersRepository.deleteContainersById("Y", containerIdsToDelete);
                        }
                        else{
                            _logger.debug("Container with ID '" + container.getId() + "' is already inside the container with ID '" + targetContainer.getId() + "'. Skipping delete...");
                        }
                    }
                    else{
                        // Container does not have any duplicates inside the target container
                        containersRepository.updateContainerParentContainerId(targetContainer.getId(), container.getId());
                    }
                }
                else{
                    numberOfIgnoredContainers++;
                    _logger.debug("Folder with ID '" + targetContainer.getId() + "' is a sub-folder of the folder with ID '" + container.getId() + "'. Skipping move...");
                }
            }
            else{
                numberOfIgnoredContainers++;
                _logger.debug("Target container ID '" + targetContainer.getId() + "' and the container with ID '" + container.getId() + "' is the same. Skipping move...");
            }
        }

        return numberOfIgnoredContainers;
    }

    @LogEntryExitExecutionTime
    public int copyContainers(DiscoveryClient discoveryClient, ContainersRepository containersRepository, ContainerAssetsRepository containerAssetsRepository,
                              AssetsRepository assetsRepository, String folderServiceLoadbalancerServiceName, String jobServiceLoadbalancerServiceName,
                              HttpSession session, List<String> sourceContainerIds, List<String> targetContainerIds, String username, String importStagingPath) throws Exception {
        int numberOfIgnoredContainers = 0;

        for(String targetContainerId: targetContainerIds){
            Container targetContainer = getContainer(containersRepository, targetContainerId);
            for(String sourceContainerId: sourceContainerIds){
                Container sourceContainer = getContainer(containersRepository, sourceContainerId);
                if(!sourceContainerId.equalsIgnoreCase(targetContainerId)){
                    if(!isSubfolder(containersRepository, targetContainer, sourceContainer)){
                        copyContainer(discoveryClient, containersRepository, containerAssetsRepository,
                                assetsRepository, folderServiceLoadbalancerServiceName, jobServiceLoadbalancerServiceName,
                                session, sourceContainer, targetContainer, username, importStagingPath);
                    }
                    else{
                        numberOfIgnoredContainers++;
                        _logger.debug("Folder with ID '" + targetContainer.getId() + "' is a sub-folder of the source container with ID '" + sourceContainer.getId() + "'. Skipping copy...");
                    }
                }
                else{
                    numberOfIgnoredContainers++;
                    _logger.debug("Target container ID '" + targetContainerId + "' and the source container with ID '" + sourceContainerId + "' is the same. Skipping copy...");
                }
            }
        }

        return numberOfIgnoredContainers;
    }

    @LogEntryExitExecutionTime
    private void copyContainer(DiscoveryClient discoveryClient, ContainersRepository containersRepository, ContainerAssetsRepository containerAssetsRepository,
                               AssetsRepository assetsRepository, String folderServiceLoadbalancerServiceName, String jobServiceLoadbalancerServiceName,
                               HttpSession session, Container sourceContainer, Container targetContainer, String username, String importStagingPath) throws Exception {

        Container duplicateContainer = findDuplicateContainer(containersRepository, targetContainer.getId(), sourceContainer.getName());
        String createdContainerId;

        if(duplicateContainer != null){
            // Container has a duplicate inside the target folder, so we don't need to create a new container
            createdContainerId = duplicateContainer.getId();
        }
        else{
            // Container does not have any duplicates inside the target container, so we create a new container
            createdContainerId = createContainer(discoveryClient, folderServiceLoadbalancerServiceName, session, sourceContainer.getName(), targetContainer.getId());
        }

        // We copy the contents to the target container
        if(StringUtils.isNotBlank(createdContainerId)){
            // We find and copy all the assets inside the folder to the new folder
            List<String> assetIdsInsideContainer = containerAssetsRepository.findContainerAssetsByContainerId(sourceContainer.getId()).stream().map(ContainerAsset::getAssetId).collect(Collectors.toList());
            List<String> getNonDeletedLastVersionAssetIds = assetsRepository.getNonDeletedLastVersionAssetsByAssetIds(assetIdsInsideContainer).stream().map(Asset::getId).collect(Collectors.toList());
            if(!getNonDeletedLastVersionAssetIds.isEmpty()){
                List<String> singleTargetContainerId = new ArrayList<>();
                singleTargetContainerId.add(createdContainerId);
                AssetUtils.getInstance().copyAssets(assetsRepository, discoveryClient, session, jobServiceLoadbalancerServiceName, getNonDeletedLastVersionAssetIds, singleTargetContainerId, username, importStagingPath);
            }

            // We find the containers which are inside the container and copy them to their new paths recursively
            List<Container> nonDeletedContainersByParentContainerId = containersRepository.getNonDeletedContainersByParentContainerId(sourceContainer.getId());

            Container createdContainer = getContainer(containersRepository, createdContainerId);

            if(!nonDeletedContainersByParentContainerId.isEmpty()){
                for(Container nonDeletedContainerByParentContainerId: nonDeletedContainersByParentContainerId){
                    copyContainer(discoveryClient, containersRepository, containerAssetsRepository,
                            assetsRepository, folderServiceLoadbalancerServiceName, jobServiceLoadbalancerServiceName,
                            session, nonDeletedContainerByParentContainerId, createdContainer, username, importStagingPath);
                }
            }
        }
        else{
            throw new IllegalArgumentException("Created container ID is blank!");
        }
    }

    @LogEntryExitExecutionTime
    public String createContainer(DiscoveryClient discoveryClient, String folderServiceLoadbalancerServiceName, HttpSession session, String name, String parentContainerId) throws Exception {
        String folderServiceLoadbalancerUrl = LoadbalancerUtils.getInstance().getLoadbalancerUrl(discoveryClient, folderServiceLoadbalancerServiceName);

        RestTemplate restTemplate = new RestTemplate();
        HttpHeaders headers = AuthenticationUtils.getInstance().getHeaders(session);

        CreateContainerRequest createContainerRequest = new CreateContainerRequest();
        createContainerRequest.setContainerName(name);
        createContainerRequest.setParentContainerId(parentContainerId);

        ResponseEntity<CreateContainerResponse> folderServiceResponseEntity = restTemplate.postForEntity(folderServiceLoadbalancerUrl + "/containers", new HttpEntity<>(createContainerRequest, headers), CreateContainerResponse.class);
        boolean successful = folderServiceResponseEntity.getStatusCode().is2xxSuccessful();

        if(successful){
            return folderServiceResponseEntity.getBody().getContainerId();
        }
        else{
            _logger.error("An error occurred while creating the container. " + folderServiceResponseEntity.getBody().getMessage());
            return null;
        }
    }

    @LogEntryExitExecutionTime
    private boolean isSubfolder(ContainersRepository containersRepository, Container subFolder, Container subFolderOf) {
        while(subFolder.getParentId() != null){
            Container parentContainer = getContainer(containersRepository, subFolder.getParentId());
            if(parentContainer.getId().equalsIgnoreCase(subFolderOf.getId())){
                return true;
            }
            else{
                subFolder = parentContainer;
            }
        }
        return false;
    }

    @LogEntryExitExecutionTime
    public Container findDuplicateContainer(ContainersRepository containersRepository, String targetContainerId, String containerName){
        List<Container> nonDeletedContainersByParentContainerId = containersRepository.getNonDeletedContainersByParentContainerId(targetContainerId);

        List<Container> duplicateNameContainers = nonDeletedContainersByParentContainerId.stream().filter(c -> c.getName().equalsIgnoreCase(containerName)).collect(Collectors.toList());
        if(!duplicateNameContainers.isEmpty()){
            if(duplicateNameContainers.size() == 1){
                return duplicateNameContainers.get(0);
            }
            else{
                throw new IllegalArgumentException("There are more than one duplicate container!");
            }
        }
        else{
            return null;
        }
    }

    @LogEntryExitExecutionTime
    public Container findDuplicateTopLevelContainer(ContainersRepository containersRepository, String containerName){
        List<Container> topLevelNonDeletedContainers = containersRepository.getTopLevelNonDeletedContainers();
        List<Container> duplicateNameContainers = topLevelNonDeletedContainers.stream().filter(c -> c.getName().equalsIgnoreCase(containerName)).collect(Collectors.toList());
        if(!duplicateNameContainers.isEmpty()){
            if(duplicateNameContainers.size() == 1){
                return duplicateNameContainers.get(0);
            }
            else{
                throw new IllegalArgumentException("There are more than one duplicate container!");
            }
        }
        else{
            return null;
        }
    }

    @LogEntryExitExecutionTime
    public Container updateContainer(ContainersRepository containersRepository, UpdateContainerRequest updateContainerRequest, String containerId){
        Container container = getContainer(containersRepository, containerId);
        boolean updateCreatedByUserName = StringUtils.isNotBlank(updateContainerRequest.getCreatedByUsername());
        boolean updateCreationDate = updateContainerRequest.getCreationDate() != null;
        boolean updateDeleted = StringUtils.isNotBlank(updateContainerRequest.getDeleted());
        boolean updateIsSystem = StringUtils.isNotBlank(updateContainerRequest.getIsSystem());
        boolean updateName = StringUtils.isNotBlank(updateContainerRequest.getName());
        boolean updateParentId = StringUtils.isNotBlank(updateContainerRequest.getParentId());

        container.setCreatedByUsername(updateCreatedByUserName ? updateContainerRequest.getCreatedByUsername() : container.getCreatedByUsername());
        container.setCreationDate(updateCreationDate ? updateContainerRequest.getCreationDate() : container.getCreationDate());
        container.setDeleted(updateDeleted ? updateContainerRequest.getDeleted() : container.getDeleted());
        container.setSystem(updateIsSystem ? updateContainerRequest.getIsSystem() : container.getSystem());
        container.setName(updateName ? updateContainerRequest.getName() : container.getName());
        container.setParentId(updateParentId ? updateContainerRequest.getParentId() : container.getParentId());
        containersRepository.save(container);

        return getContainer(containersRepository, containerId);
    }
}