package com.github.murataykanat.toybox.utilities;

import com.github.murataykanat.toybox.annotations.LogEntryExitExecutionTime;
import com.github.murataykanat.toybox.dbo.*;
import com.github.murataykanat.toybox.repositories.AssetsRepository;
import com.github.murataykanat.toybox.repositories.ContainerAssetsRepository;
import com.github.murataykanat.toybox.repositories.ContainerUsersRepository;
import com.github.murataykanat.toybox.repositories.ContainersRepository;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

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
    public Container getContainer(ContainersRepository containersRepository, String containerId) throws Exception {
        List<Container> containersById = containersRepository.getContainersById(containerId);
        if(!containersById.isEmpty()){
            if(containersById.size() == 1){
                return containersById.get(0);
            }
            else{
                throw new Exception("There are multiple containers with ID '" + containerId + "'!");
            }
        }
        else{
            throw new Exception("There is no container with ID '" + containerId + "'!");
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
    public void moveContainers(ContainersRepository containersRepository, ContainerAssetsRepository containerAssetsRepository, AssetsRepository assetsRepository, List<String> containerIds, Container targetContainer) throws Exception {
        for(String containerId: containerIds){
            Container container = ContainerUtils.getInstance().getContainer(containersRepository, containerId);

            Container duplicateContainer = null;

            List<Container> nonDeletedContainersByParentContainerId = containersRepository.getNonDeletedContainersByParentContainerId(targetContainer.getId());
            // TODO: Prevent folders to be moved into folders which are below them in the folder hierarchy
            if(!container.getId().equalsIgnoreCase(targetContainer.getId())){
                List<Container> duplicateNameContainers = nonDeletedContainersByParentContainerId.stream().filter(c -> c.getName().equalsIgnoreCase(container.getName())).collect(Collectors.toList());
                if(!duplicateNameContainers.isEmpty()){
                    if(duplicateNameContainers.size() == 1){
                        // Container has a duplicate inside the target folder
                        // We delete the container and move the contents to the duplicate container
                        duplicateContainer = duplicateNameContainers.get(0);

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
                        throw new Exception("There are more than one duplicate container!");
                    }
                }
                else{
                    // Container does not have any duplicates inside the target container
                    containersRepository.updateContainerParentContainerId(targetContainer.getId(), container.getId());
                }
            }
            else{
                _logger.debug("Target container ID '" + targetContainer.getId() + "' and the container with ID '" + container.getId() + "' is the same. Skipping move...");
            }
        }
    }
}
