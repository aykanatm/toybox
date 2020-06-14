package com.github.murataykanat.toybox.utilities;

import com.github.murataykanat.toybox.annotations.LogEntryExitExecutionTime;
import com.github.murataykanat.toybox.contants.ToyboxConstants;
import com.github.murataykanat.toybox.dbo.*;
import com.github.murataykanat.toybox.predicates.ToyboxPredicateBuilder;
import com.github.murataykanat.toybox.predicates.ToyboxStringPath;
import com.github.murataykanat.toybox.repositories.*;
import com.github.murataykanat.toybox.schema.container.Breadcrumb;
import com.github.murataykanat.toybox.schema.container.CreateContainerRequest;
import com.github.murataykanat.toybox.schema.container.CreateContainerResponse;
import com.github.murataykanat.toybox.schema.container.UpdateContainerRequest;
import com.github.murataykanat.toybox.schema.notification.SendNotificationRequest;
import com.github.murataykanat.toybox.schema.search.SearchCondition;
import com.google.common.collect.Lists;
import com.querydsl.core.types.OrderSpecifier;
import org.apache.commons.lang.RandomStringUtils;
import org.apache.commons.lang.StringUtils;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;

import javax.servlet.http.HttpSession;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Function;
import java.util.function.Predicate;
import java.util.stream.Collectors;

@Component
public class ContainerUtils {
    private static final Log _logger = LogFactory.getLog(ContainerUtils.class);

    @Autowired
    private AssetUtils assetUtils;
    @Autowired
    private LoadbalancerUtils loadbalancerUtils;
    @Autowired
    private AuthenticationUtils authenticationUtils;
    @Autowired
    private NotificationUtils notificationUtils;
    @Autowired
    private ShareUtils shareUtils;

    @Autowired
    private AssetsRepository assetsRepository;
    @Autowired
    private ContainersRepository containersRepository;
    @Autowired
    private ContainerAssetsRepository containerAssetsRepository;
    @Autowired
    private ContainerUsersRepository containerUsersRepository;

    @LogEntryExitExecutionTime
    @SuppressWarnings("unchecked")
    public List<Container> getContainers(List<SearchCondition> searchConditions, String sortField, String sortType){
        OrderSpecifier<?> order;
        ToyboxPredicateBuilder<Container> builder = new ToyboxPredicateBuilder().with(searchConditions, Container.class);

        if(ToyboxConstants.SORT_TYPE_ASCENDING.equalsIgnoreCase(sortType)){
            if("importDate".equalsIgnoreCase(sortField)){
                order = QContainer.container.importDate.asc();
            }
            else{
                order = new ToyboxStringPath(QAsset.asset, sortField).asc();
            }
        }
        else if(ToyboxConstants.SORT_TYPE_DESCENDING.equalsIgnoreCase(sortType)){
            if("importDate".equalsIgnoreCase(sortField)){
                order = QContainer.container.importDate.desc();
            }
            else{
                order = new ToyboxStringPath(QAsset.asset, sortField).desc();
            }
        }
        else{
            _logger.warn("Sort type '" + sortType + "' is invalid, falling back to default.");
            order = new ToyboxStringPath(QAsset.asset, sortField).desc();
        }

        Iterable<Container> iterableContainers = containersRepository.findAll(builder.build(), order);
        return Lists.newArrayList(iterableContainers);
    }

    @LogEntryExitExecutionTime
    public Container getContainer(String containerId) {
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

    public List<Container> getContainersByContainerIds(List<String> containerIds){
        if(!containerIds.isEmpty()){
            return containersRepository.getContainersByContainerIds(containerIds);
        }

        return new ArrayList<>();
    }

    @LogEntryExitExecutionTime
    public String getParentContainerId(Asset asset){
        List<ContainerAsset> containerAssetsByAssetId = containerAssetsRepository.findContainerAssetsByAssetId(asset.getId());
        if(!containerAssetsByAssetId.isEmpty()){
            if(containerAssetsByAssetId.size() == 1){
                return containerAssetsByAssetId.get(0).getContainerId();
            }
            else{
                throw new IllegalArgumentException("Asset is in multiple folders!");
            }
        }
        else{
            throw new IllegalArgumentException("Asset is not in any folder!");
        }
    }

    @LogEntryExitExecutionTime
    public List<Container> getSubContainerTree(List<Container> result, String containerId){
        List<Container> nonDeletedContainersByParentContainerId = containersRepository.getNonDeletedContainersByParentContainerId(containerId);

        if(nonDeletedContainersByParentContainerId.isEmpty()){
            return result;
        }

        result.addAll(nonDeletedContainersByParentContainerId);

        for (Container container: nonDeletedContainersByParentContainerId){
            result.addAll(getSubContainerTree(result, container.getId()));
        }

        return result.stream().filter(distinctByKey(Container::getId)).collect(Collectors.toList());
    }

    @LogEntryExitExecutionTime
    private static <T> Predicate<T> distinctByKey(Function<? super T, ?> keyExtractor) {
        Set<Object> seen = ConcurrentHashMap.newKeySet();
        return t -> seen.add(keyExtractor.apply(t));
    }

    @LogEntryExitExecutionTime
    public boolean isSubscribed(User user, Container container){
        List<ContainerUser> containerUsersByUserId = containerUsersRepository.findContainerUsersByUserId(user.getId());
        for(ContainerUser containerUser: containerUsersByUserId){
            if(containerUser.getContainerId().equalsIgnoreCase(container.getId())){
                return true;
            }
        }

        return false;
    }

    @LogEntryExitExecutionTime
    public int moveContainers(List<String> containerIds, Container targetContainer, User user, HttpSession session) throws Exception {
        int numberOfIgnoredContainers = 0;

        for(String containerId: containerIds){
            Container container = getContainer(containerId);
            if(!container.getId().equalsIgnoreCase(targetContainer.getId())){
                if(!isSubfolder(targetContainer, container)){
                    Container duplicateContainer = findDuplicateContainer(targetContainer.getId(), container.getName());
                    if(duplicateContainer != null){
                        // Container has a duplicate inside the target folder
                        // We delete the container and move the contents to the duplicate container
                        List<String> assetIdsInsideContainer = containerAssetsRepository.findContainerAssetsByContainerId(container.getId()).stream().map(ContainerAsset::getAssetId).collect(Collectors.toList());
                        List<String> getNonDeletedLastVersionAssetIds = assetsRepository.getNonDeletedLastVersionAssetsByAssetIds(assetIdsInsideContainer).stream().map(Asset::getId).collect(Collectors.toList());
                        assetUtils.moveAssets(getNonDeletedLastVersionAssetIds, duplicateContainer, user, session);

                        List<String> containerIdsToDelete = new ArrayList<>();
                        containerIdsToDelete.add(container.getId());

                        if(!targetContainer.getId().equalsIgnoreCase(container.getParentId())){
                            containersRepository.deleteContainersById(ToyboxConstants.LOOKUP_YES, containerIdsToDelete);
                        }
                        else{
                            _logger.debug("Container with ID '" + container.getId() + "' is already inside the container with ID '" + targetContainer.getId() + "'. Skipping delete...");
                        }
                    }
                    else{
                        // Container does not have any duplicates inside the target container
                        containersRepository.updateContainerParentContainerId(targetContainer.getId(), container.getId());
                    }

                    String message = "Folder '" + container.getName() + "' is moved to folder '" + targetContainer.getName() + "' by '" + user.getUsername() + "'";
                    // Send notification for subscribers
                    List<User> subscribers = getSubscribers(container.getId());
                    for(User subscriber: subscribers){
                        SendNotificationRequest sendNotificationRequest = new SendNotificationRequest();
                        sendNotificationRequest.setFromUsername(user.getUsername());
                        sendNotificationRequest.setToUsername(subscriber.getUsername());
                        sendNotificationRequest.setMessage(message);
                        notificationUtils.sendNotification(sendNotificationRequest, session);
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
    public int copyContainers(HttpSession session, List<String> sourceContainerIds, List<String> targetContainerIds, String username, String importStagingPath) throws Exception {
        int numberOfIgnoredContainers = 0;

        for(String targetContainerId: targetContainerIds){
            Container targetContainer = getContainer(targetContainerId);
            for(String sourceContainerId: sourceContainerIds){
                Container sourceContainer = getContainer(sourceContainerId);
                if(!sourceContainerId.equalsIgnoreCase(targetContainerId)){
                    if(!isSubfolder(targetContainer, sourceContainer)){
                        copyContainer(session, sourceContainer, targetContainer, username, importStagingPath);
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
    private void copyContainer(HttpSession session, Container sourceContainer, Container targetContainer, String username, String importStagingPath) throws Exception {
        Container duplicateContainer = findDuplicateContainer(targetContainer.getId(), sourceContainer.getName());
        String createdContainerId;

        if(duplicateContainer != null){
            // Container has a duplicate inside the target folder, so we don't need to create a new container
            createdContainerId = duplicateContainer.getId();
        }
        else{
            // Container does not have any duplicates inside the target container, so we create a new container
            createdContainerId = createContainer(session, sourceContainer.getName(), targetContainer.getId());
        }


        if(StringUtils.isNotBlank(createdContainerId)){
            Container createdContainer = getContainer(createdContainerId);
            User user = authenticationUtils.getUser(username);

            String message = "Folder '" + sourceContainer.getName() + "' is copied to folder '" + targetContainer.getName() + "' by '" + user.getUsername() + "'";

            // Send notification for subscribers
            List<User> subscribers = getSubscribers(sourceContainer.getId());
            for(User subscriber: subscribers){
                SendNotificationRequest sendNotificationRequest = new SendNotificationRequest();
                sendNotificationRequest.setFromUsername(user.getUsername());
                sendNotificationRequest.setToUsername(subscriber.getUsername());
                sendNotificationRequest.setMessage(message);
                notificationUtils.sendNotification(sendNotificationRequest, session);
            }

            // Send notification for container owners
            List<InternalShare> internalShares = shareUtils.getInternalSharesWithTargetUser(user.getId(), sourceContainer.getId(), false);
            for(InternalShare internalShare: internalShares){
                if(internalShare.getNotifyOnCopy().equalsIgnoreCase(ToyboxConstants.LOOKUP_YES)){
                    SendNotificationRequest sendNotificationRequest = new SendNotificationRequest();
                    sendNotificationRequest.setFromUsername(user.getUsername());
                    sendNotificationRequest.setToUsername(internalShare.getUsername());
                    sendNotificationRequest.setMessage(message);
                    notificationUtils.sendNotification(sendNotificationRequest, session);
                }
            }

            // We copy the contents to the target container
            // We find and copy all the assets inside the folder to the new folder
            List<String> assetIdsInsideContainer = containerAssetsRepository.findContainerAssetsByContainerId(sourceContainer.getId()).stream().map(ContainerAsset::getAssetId).collect(Collectors.toList());
            if(!assetIdsInsideContainer.isEmpty()){
                List<String> getNonDeletedLastVersionAssetIds = assetsRepository.getNonDeletedLastVersionAssetsByAssetIds(assetIdsInsideContainer).stream().map(Asset::getId).collect(Collectors.toList());
                if(!getNonDeletedLastVersionAssetIds.isEmpty()){
                    List<String> singleTargetContainerId = new ArrayList<>();
                    singleTargetContainerId.add(createdContainerId);
                    assetUtils.copyAssets(session, getNonDeletedLastVersionAssetIds, singleTargetContainerId, username, importStagingPath);
                }
            }

            // We find the containers which are inside the container and copy them to their new paths recursively
            List<Container> nonDeletedContainersByParentContainerId = containersRepository.getNonDeletedContainersByParentContainerId(sourceContainer.getId());

            if(!nonDeletedContainersByParentContainerId.isEmpty()){
                for(Container nonDeletedContainerByParentContainerId: nonDeletedContainersByParentContainerId){
                    copyContainer(session, nonDeletedContainerByParentContainerId, createdContainer, username, importStagingPath);
                }
            }
        }
        else{
            throw new IllegalArgumentException("Created container ID is blank!");
        }
    }

    @LogEntryExitExecutionTime
    public String createContainer(HttpSession session, String name, String parentContainerId) {
        String folderServiceLoadbalancerUrl = loadbalancerUtils.getLoadbalancerUrl(ToyboxConstants.FOLDER_SERVICE_LOAD_BALANCER_SERVICE_NAME, ToyboxConstants.FOLDER_SERVICE_NAME, session, false);

        RestTemplate restTemplate = new RestTemplate();
        HttpHeaders headers = authenticationUtils.getHeaders(session);

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
    private boolean isSubfolder(Container subFolder, Container subFolderOf) {
        while(subFolder.getParentId() != null){
            Container parentContainer = getContainer(subFolder.getParentId());
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
    public Container findDuplicateContainer(String targetContainerId, String containerName){
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
    public Container findDuplicateTopLevelContainer(String containerName){
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
    public void deleteContainers(List<Container> containers, User user, HttpSession session) throws Exception {
        UpdateContainerRequest updateContainerRequest = new UpdateContainerRequest();
        updateContainerRequest.setDeleted(ToyboxConstants.LOOKUP_YES);
        for(Container container: containers){
            updateContainer(updateContainerRequest, container.getId(), user, session);
        }
    }

    @LogEntryExitExecutionTime
    public void restoreContainers(List<Container> containers, User user, HttpSession session) throws Exception {
        UpdateContainerRequest updateContainerRequest = new UpdateContainerRequest();
        updateContainerRequest.setDeleted(ToyboxConstants.LOOKUP_NO);
        for(Container container: containers){
            updateContainer(updateContainerRequest, container.getId(), user, session);
        }
    }

    @LogEntryExitExecutionTime
    public void purgeContainers(List<Container> containers) throws IOException {
        List<Asset> assetsToBePurged = new ArrayList<>();
        for(Container container: containers){
            List<ContainerAsset> containerAssetsByContainerId = containerAssetsRepository.findContainerAssetsByContainerId(container.getId());
            for(ContainerAsset containerAsset: containerAssetsByContainerId){
                assetsToBePurged.add(assetUtils.getAsset(containerAsset.getAssetId()));
            }
        }
        assetUtils.purgeAssets(assetsToBePurged);
        containersRepository.delete(containers);
    }

    @LogEntryExitExecutionTime
    public Container updateContainer(UpdateContainerRequest updateContainerRequest, String containerId, User user, HttpSession session) throws Exception {
        Container container = getContainer(containerId);
        String oldContainerName = container.getName();

        boolean updateCreatedByUserName = StringUtils.isNotBlank(updateContainerRequest.getCreatedByUsername());
        boolean updateCreationDate = updateContainerRequest.getCreationDate() != null;
        boolean updateDeleted = StringUtils.isNotBlank(updateContainerRequest.getDeleted());
        boolean updateIsSystem = StringUtils.isNotBlank(updateContainerRequest.getIsSystem());
        boolean updateName = StringUtils.isNotBlank(updateContainerRequest.getName());
        boolean updateParentId = StringUtils.isNotBlank(updateContainerRequest.getParentId());

        container.setCreatedByUsername(updateCreatedByUserName ? updateContainerRequest.getCreatedByUsername() : container.getCreatedByUsername());
        container.setImportDate(updateCreationDate ? updateContainerRequest.getCreationDate() : container.getImportDate());
        container.setDeleted(updateDeleted ? updateContainerRequest.getDeleted() : container.getDeleted());
        container.setSystem(updateIsSystem ? updateContainerRequest.getIsSystem() : container.getSystem());
        container.setName(updateName ? updateContainerRequest.getName() : container.getName());
        container.setParentId(updateParentId ? updateContainerRequest.getParentId() : container.getParentId());
        containersRepository.save(container);

        String message = "Folder '" + oldContainerName + "' is updated by '" + user.getUsername() + "'";

        // Send notification for subscribers
        List<User> subscribers = getSubscribers(container.getId());
        for(User subscriber: subscribers){
            SendNotificationRequest sendNotificationRequest = new SendNotificationRequest();
            sendNotificationRequest.setFromUsername(user.getUsername());
            sendNotificationRequest.setToUsername(subscriber.getUsername());
            sendNotificationRequest.setMessage(message);
            notificationUtils.sendNotification(sendNotificationRequest, session);
        }

        // Send notification for container owners
        List<InternalShare> internalShares = shareUtils.getInternalSharesWithTargetUser(user.getId(), container.getId(), false);
        for(InternalShare internalShare: internalShares){
            if(internalShare.getNotifyOnEdit().equalsIgnoreCase(ToyboxConstants.LOOKUP_YES)){
                SendNotificationRequest sendNotificationRequest = new SendNotificationRequest();
                sendNotificationRequest.setFromUsername(user.getUsername());
                sendNotificationRequest.setToUsername(internalShare.getUsername());
                sendNotificationRequest.setMessage(message);
                notificationUtils.sendNotification(sendNotificationRequest, session);
            }
        }

        return getContainer(containerId);
    }

    @LogEntryExitExecutionTime
    public void createContainer(Container container){
        container.setId(generateFolderId());
        _logger.debug("Container ID: " + container.getId());
        _logger.debug("Container name: " + container.getName());
        _logger.debug("Container parent ID: " + container.getParentId());
        _logger.debug("Container created by username: " + container.getCreatedByUsername());
        _logger.debug("Container creation date: " + container.getImportDate());

        containersRepository.insertContainer(container.getId(), container.getName(), container.getParentId(),
                container.getCreatedByUsername(), container.getImportDate(), container.getDeleted(),
                container.getSystem());
    }

    @LogEntryExitExecutionTime
    public List<User> getSubscribers(String containerId){
        List<User> subscribers = new ArrayList<>();

        List<ContainerUser> containerUsersByContainerId = containerUsersRepository.findContainerUsersByContainerId(containerId);

        for(ContainerUser containerUser: containerUsersByContainerId){
            User user = authenticationUtils.getUser(containerUser.getUserId());
            subscribers.add(user);
        }

        return subscribers;
    }

    @LogEntryExitExecutionTime
    public boolean subscribeToContainer(String containerId, User user){
        Container container = getContainer(containerId);

        if(!isSubscribed(user, container)){
            containerUsersRepository.insertSubscriber(container.getId(), user.getId());

            List<ContainerAsset> containerAssetsByContainerId = containerAssetsRepository.findContainerAssetsByContainerId(container.getId());
            for(ContainerAsset containerAsset: containerAssetsByContainerId){
                assetUtils.subscribeToAsset(containerAsset.getAssetId(), user);
            }
            List<Container> subContainerTree = getSubContainerTree(new ArrayList<>(), containerId);
            for(Container subContainer: subContainerTree){
                subscribeToContainer(subContainer.getId(), user);
            }
            return true;
        }

        return false;
    }

    @LogEntryExitExecutionTime
    public boolean unsubscribeFromContainer(String containerId, User user) throws Exception {
        Container container = getContainer(containerId);
        if(isSubscribed(user, container)){
            User createUser = authenticationUtils.getUser(container.getCreatedByUsername());
            containerUsersRepository.deleteSubscriber(container.getId(), createUser.getId());

            List<Asset> containerAssets = getContainerAssets(containerId);
            for(Asset containerAsset: containerAssets){
                assetUtils.unsubscribeFromAsset(containerAsset.getId(), user);
            }

            List<Container> subContainerTree = getSubContainerTree(new ArrayList<>(), containerId);
            for(Container subContainer: subContainerTree){
                unsubscribeFromContainer(subContainer.getId(), user);
            }

            return true;
        }

        return false;
    }

    @LogEntryExitExecutionTime
    public void unsubscribeUsersFromContainer(String containerId){
        containerUsersRepository.deleteAllSubscribersByContainerId(containerId);

        List<Asset> containerAssets = getContainerAssets(containerId);
        for(Asset containerAsset: containerAssets){
            assetUtils.unsubscribeUsersFromAsset(containerAsset.getId());
        }

        List<Container> subContainerTree = getSubContainerTree(new ArrayList<>(), containerId);
        for(Container subContainer: subContainerTree){
            unsubscribeUsersFromContainer(subContainer.getId());
        }
    }

    @LogEntryExitExecutionTime
    private String generateFolderId(){
        String containerId = RandomStringUtils.randomAlphanumeric(ToyboxConstants.FOLDER_ID_LENGTH);
        if(isContainerIdValid(containerId)){
            return containerId;
        }
        return generateFolderId();
    }

    @LogEntryExitExecutionTime
    private boolean isContainerIdValid(String containerId){
        List<Container> containers = containersRepository.getContainersById(containerId);
        List<Asset> assets = assetsRepository.getAssetsById(containerId);
        if(containers.isEmpty() && assets.isEmpty()){
            return true;
        }
        else{
            return false;
        }
    }

    @LogEntryExitExecutionTime
    public List<Breadcrumb> generateContainerPath(String containerId, String username) {
        List<Breadcrumb> breadcrumbs = new ArrayList<>();
        Container container;
        Breadcrumb breadcrumb = new Breadcrumb();

        if(StringUtils.isNotBlank(containerId) && !containerId.equalsIgnoreCase("null")){
            container = getContainer(containerId);
            breadcrumb.setContainerId(container.getId());
            breadcrumb.setContainerName(container.getName());
            breadcrumbs.add(breadcrumb);

            while (StringUtils.isNotBlank(container.getParentId())){
                container = getContainer(container.getParentId());

                if(container.getSystem().equalsIgnoreCase(ToyboxConstants.LOOKUP_YES) && !containerId.equalsIgnoreCase(username)){
                    breadcrumb = new Breadcrumb();
                    breadcrumb.setContainerId(username);
                    breadcrumb.setContainerName(username);

                    breadcrumbs.add(breadcrumb);
                }
                else{
                    breadcrumb = new Breadcrumb();
                    breadcrumb.setContainerId(container.getId());
                    breadcrumb.setContainerName(container.getName());

                    breadcrumbs.add(breadcrumb);
                }
            }
        }

        breadcrumb = new Breadcrumb();
        breadcrumb.setContainerId(null);
        breadcrumb.setContainerName("Root");

        breadcrumbs.add(breadcrumb);

        return breadcrumbs;
    }

    public List<Asset> getContainerAssets(String containerId){
        List<ContainerAsset> containerAssetsByContainerId = containerAssetsRepository.findContainerAssetsByContainerId(containerId);
        List<String> assetIds = containerAssetsByContainerId.stream().map(ContainerAsset::getAssetId).collect(Collectors.toList());
        if(!assetIds.isEmpty()){
            return assetUtils.getAssetsByAssetIds(assetIds);
        }
        return new ArrayList<>();
    }
}