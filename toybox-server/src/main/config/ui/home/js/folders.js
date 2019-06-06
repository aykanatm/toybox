const folders = new Vue({
    el: '#toybox-folders',
    mixins:[paginationMixin, facetMixin, messageMixin, userMixin, serviceMixin, itemActionsMixin],
    data:{
        view: 'folders',
        items:[],
        breadcrumbs:[],
        selectedItems: [],
        isLoading: false,
        renditionUrl:'',
        canCreateFolder: false,
        canUploadFile: false,
        currentFolderId: '',
        // Sorting
        defaultSortType: 'des',
        defaultSortColumn: 'asset_import_date',
        sortType: 'des',
        sortColumn: 'asset_import_date',
        sortedAscByAssetName: false,
        sortedDesByAssetName: false,
        sortedAscByAssetImportDate: false,
        sortedDesByAssetImportDate: false,
    },
    mounted:function(){
        var csrfHeader = $("meta[name='_csrf_header']").attr("content");
        var csrfToken = $("meta[name='_csrf']").attr("content");
        console.log('CSRF header: ' + csrfHeader);
        console.log('CSRF token: ' + csrfToken);
        axios.defaults.headers = {
            // 'X-CSRF-TOKEN': csrfToken,
            'XSRF-TOKEN': csrfToken
        }
        axios.defaults.withCredentials = true;

        this.getService("toybox-rendition-loadbalancer")
            .then(response => {
                this.renditionUrl = response.data.value;
            });

        // Initialize accordions
        $('.ui.accordion').accordion();

        // Initialize event listeners
        this.$root.$on('perform-faceted-search', (facet, isAdd) => {
            if(isAdd){
                console.log('Adding facet ' + facet.fieldName + ' and its value ' + facet.fieldValue + ' to search');
                this.searchRequestFacetList.push(facet);
            }
            else{
                console.log('Removing facet ' + facet.fieldName + ' and its value ' + facet.fieldValue + ' from search');
                var index;
                for(var i = 0; i < this.searchRequestFacetList.length; i++){
                    var assetRequestFacet = this.searchRequestFacetList[i];
                    if(assetRequestFacet.fieldName === facet.fieldName && assetRequestFacet.fieldValue === facet.fieldValue){
                        index = i;
                        break;
                    }
                }
                this.searchRequestFacetList.splice(index, 1);
            }

            this.getItems(this.currentFolderId, this.offset, this.limit, this.sortType, this.sortColumn, this.searchRequestFacetList);
        });

        this.$root.$on('item-selection-changed', this.onItemSelectionChanged);
        this.$root.$on('message-sent', this.displayMessage);
        this.$root.$on('refresh-items', this.refreshItems);
        this.$root.$on('open-folder', this.openFolder)

        setTimeout(() => {
            if(this.user.isAdmin){
                this.getTopLevelFolders(this.offset, this.limit);
            }
            else{
                this.getItems(this.currentFolderId, this.offset, this.limit, this.sortType, this.sortColumn, this.searchRequestFacetList)
            }
        }, 200);
    },
    methods:{
        getTopLevelFolders:function(offset, limit){
            this.isLoading = true;
            this.getService("toybox-folder-loadbalancer")
            .then(response => {
                if(response){
                    var searchRequest = {};
                    searchRequest.limit = limit;
                    searchRequest.offset = offset;
                    searchRequest.retrieveTopLevelContainers = 'Y';

                    return axios.post(response.data.value + "/containers/search", searchRequest)
                        .catch(error => {
                            this.isLoading = false;
                            var errorMessage;

                            if(error.response){
                                errorMessage = error.response.data.message
                                if(error.response.status == 401){
                                    window.location = '/logout';
                                }
                            }
                            else{
                                errorMessage = error.message;
                            }

                            console.error(errorMessage);
                            this.$root.$emit('message-sent', 'Error', errorMessage);
                        });
                }
            })
            .then(response => {
                console.log(response);
                if(response){
                    this.isLoading = false;
                    this.items = response.data.containers;
                    this.updateBreadcrumbs(response.data.breadcrumbs);

                    if(this.items == null || this.items.length == 0){
                        this.displayMessage('Information','You do not have any folders.');
                        this.totalRecords = 0;
                        this.totalPages = 0;
                        this.currentPage = 0;
                    }
                    else{
                        this.totalRecords = response.data.totalRecords;
                        this.totalPages = Math.ceil(this.totalRecords / this.limit);
                        this.currentPage = Math.ceil((offset / limit) + 1);
                    }

                    this.currentFolderId = response.data.containerId;

                    this.updateButtons();
                    this.updatePagination(this.currentPage, this.totalPages, offset, limit, this.totalRecords);
                }
            });
        },
        openFolder:function(folder){
            this.currentFolderId = folder.id;
            this.searchRequestFacetList = [];

            if(this.currentFolderId === undefined || this.currentFolderId === '' || this.currentFolderId === 'null' || this.currentFolderId === null){
                this.getTopLevelFolders(this.offset, this.limit);
            }
            else{
                this.getItems(this.currentFolderId, this.offset, this.limit, this.sortType, this.sortColumn, this.searchRequestFacetList)
            }
        },
        getItems:function(containerId, offset, limit, sortType, sortColumn, searchRequestFacetList){
            this.isLoading = true;
            if(containerId){
                this.getService("toybox-folder-loadbalancer")
                .then(response => {
                    if(response){
                        var searchRequest = {};
                        searchRequest.limit = limit;
                        searchRequest.offset = offset;
                        searchRequest.sortType = sortType;
                        searchRequest.sortColumn = sortColumn;
                        searchRequest.assetSearchRequestFacetList = searchRequestFacetList;

                        return axios.post(response.data.value + '/containers/' + containerId + '/search', searchRequest)
                            .catch(error => {
                                this.isLoading = false;
                                var errorMessage;

                                if(error.response){
                                    errorMessage = error.response.data.message
                                    if(error.response.status == 401){
                                        window.location = '/logout';
                                    }
                                }
                                else{
                                    errorMessage = error.message;
                                }

                                console.error(errorMessage);
                                this.$root.$emit('message-sent', 'Error', errorMessage);
                            });
                    }
                })
                .then(response => {
                    console.log(response);
                    if(response){
                        this.isLoading = false;
                        this.items = response.data.containerItems;
                        this.facets = response.data.facets;
                        this.updateBreadcrumbs(response.data.breadcrumbs);

                        if(this.items == null || this.items.length == 0){
                            this.displayMessage('Information','You do not have any files or folders.');
                            this.totalRecords = 0;
                            this.totalPages = 0;
                            this.currentPage = 0;
                        }
                        else{
                            this.totalRecords = response.data.totalRecords;
                            this.totalPages = Math.ceil(this.totalRecords / this.limit);
                            this.currentPage = Math.ceil((offset / limit) + 1);
                        }

                        this.currentFolderId = containerId;

                        this.updateButtons();
                        this.updatePagination(this.currentPage, this.totalPages, offset, limit, this.totalRecords);
                    }
                });
            }
            else{
                this.getService("toybox-folder-loadbalancer")
                    .then(response => {
                        var container ={
                            '@class': 'com.github.murataykanat.toybox.dbo.Container',
                            'name': this.user.username,
                            'isSystem': 'Y'
                        }

                        var searchRequest = {
                            'limit': limit,
                            'offset': offset,
                            'container': container
                        }

                        var containerServiceUrl = response.data.value

                        axios.post(containerServiceUrl + "/containers/search", searchRequest)
                            .then(response => {
                                var containers = response.data.containers;
                                if(containers.length > 0){
                                    if(containers.length == 1){
                                        var userContainer = containers[0];
                                        var searchRequest = {
                                            'limit': limit,
                                            'offset': offset,
                                            'sortType': sortType,
                                            'sortColumn': sortColumn,
                                            'assetSearchRequestFacetList': searchRequestFacetList
                                        };

                                        axios.post(containerServiceUrl + '/containers/' + userContainer.id + '/search', searchRequest)
                                            .then(response => {
                                                console.log(response);
                                                if(response){
                                                    this.isLoading = false;
                                                    this.items = response.data.containerItems;
                                                    this.facets = response.data.facets;
                                                    this.updateBreadcrumbs(response.data.breadcrumbs);

                                                    if(this.items == null || this.items.length == 0){
                                                        this.displayMessage('Information','You do not have any files or folders.');
                                                        this.totalRecords = 0;
                                                        this.totalPages = 0;
                                                        this.currentPage = 0;
                                                    }
                                                    else{
                                                        this.totalRecords = response.data.totalRecords;
                                                        this.totalPages = Math.ceil(this.totalRecords / this.limit);
                                                        this.currentPage = Math.ceil((offset / limit) + 1);
                                                    }

                                                    this.currentFolderId = userContainer.id;

                                                    this.updateButtons();
                                                    this.updatePagination(this.currentPage, this.totalPages, offset, limit, this.totalRecords);
                                                }
                                            })
                                            .catch(error => {
                                                this.isLoading = false;
                                                var errorMessage;

                                                if(error.response){
                                                    errorMessage = error.response.data.message
                                                    if(error.response.status == 401){
                                                        window.location = '/logout';
                                                    }
                                                }
                                                else{
                                                    errorMessage = error.message;
                                                }

                                                console.error(errorMessage);
                                                this.$root.$emit('message-sent', 'Error', errorMessage);
                                            });
                                    }
                                    else{
                                        this.isLoading = false;
                                        var errorMessage = 'There are multiple root folders for user "' + user.username + '"';
                                        this.$root.$emit('message-sent', 'Error', errorMessage);
                                    }
                                }
                                else{
                                    this.isLoading = false;
                                    var errorMessage = 'There is no root folder for user "' + user.username + '"';
                                    this.$root.$emit('message-sent', 'Error', errorMessage);
                                }
                            })
                            .catch(error => {
                                this.isLoading = false;
                                var errorMessage;

                                if(error.response){
                                    errorMessage = error.response.data.message
                                    if(error.response.status == 401){
                                        window.location = '/logout';
                                    }
                                }
                                else{
                                    errorMessage = error.message;
                                }

                                console.error(errorMessage);
                                this.$root.$emit('message-sent', 'Error', errorMessage);
                            });
                        })
            }
        },
        onItemSelectionChanged:function(item){
            if(item.isSelected){
                this.selectedItems.push(item);
            }
            else{
                this.selectedItems.splice(item, 1);
            }

            console.log(this.selectedAssets);
        },
        itemsShare:function(){
            console.log('Sharing the following assets:');
            var selectedItems = this.getSelectedItems();
        },
        itemsDownload:function(){
            console.log('Downloading the following assets:');
            var selectedItems = this.getSelectedItems();
            this.downloadItems(selectedItems);
        },
        itemsCopy:function(){
            console.log('Copying the following assets:');
            var selectedItems = this.getSelectedItems();
        },
        itemsMove:function(){
            console.log('Moving the following assets:');
            var selectedItems = this.getSelectedItems();
        },
        itemsSubscribe:function(){
            console.log('Subscribing to the following assets:');
            var selectedItems = this.getSelectedItems();
            this.subscribeToItems(selectedItems);
        },
        itemsUnsubscribe:function(){
            console.log('Unsubscribing from the following assets:');
            var selectedItems = this.getSelectedItems();
            this.unsubscribeFromItems(selectedItems);
        },
        itemsDelete:function(){
            console.log('Deleting the following assets:');
            var selectedItems = this.getSelectedItems();
            this.deleteItems(selectedItems);
        },
        refreshItems:function(){
            this.selectedAssets = [];
            for(var i = 0; i < this.assets.length; i++){
                var asset = this.assets[i];
                this.$root.$emit('deselect-asset', asset.id);
            }

            this.getItems(this.offset, this.limit, this.sortType, this.sortColumn, this.searchRequestFacetList);
        },
        createNewFolder:function(){
            this.getService("toybox-folder-loadbalancer")
            .then(response => {
                if(response){
                    this.$root.$emit('open-create-container-modal-window', this.currentFolderId, response.data.value);
                }
            })
            .catch(error => {
                var errorMessage;

                if(error.response){
                    errorMessage = error.response.data.message
                    if(error.response.status == 401){
                        window.location = '/logout';
                    }
                }
                else{
                    errorMessage = error.message;
                }

                console.error(errorMessage);
                this.$root.$emit('message-sent', 'Error', errorMessage);
            });
        },
        uploadNewFile:function(){
            this.$root.$emit('open-import-modal-window', this.currentFolderId);
        },
        getSelectedItems:function(){
            var selectedItems = []
            for(var i = 0; i < this.selectedItems.length; i++)
            {
                var item = this.selectedItems[i];
                console.log(item.name + ' / ' +  item.id);
                selectedItems.push({id:item.id, name:item.name, type:item.type, originalAssetId:item.originalAssetId});
            }

            return selectedItems;
        },
        updateButtons:function(){
            if(this.currentFolderId && this.currentFolderId !== ''){
                this.canCreateFolder = true;
                this.canUploadFile = true;
            }
            else{
                this.canCreateFolder = false;
                this.canUploadFile = false;
            }
        },
        updateBreadcrumbs:function(breadcrumbs){
            var displayedBreadcrumbs = [];
            for(var i = breadcrumbs.length - 1; i >= 0 ; i--){
                var breadcrumb = breadcrumbs[i];
                if(i == 0){
                    breadcrumb.isSection = true;
                    breadcrumb.isActive = true;

                    displayedBreadcrumbs.push(breadcrumb);
                }
                else{
                    breadcrumb.isSection = true;
                    breadcrumb.isActive = false;
                    displayedBreadcrumbs.push(breadcrumb);

                    var dividerBreadcrumb = {
                        'id': '' + i,
                        'name': 'divider',
                        'isActive': false,
                        'isSection': false,
                        'isRoot': false
                    }
                    displayedBreadcrumbs.push(dividerBreadcrumb);
                }
            }

            this.breadcrumbs = displayedBreadcrumbs;
        }
    },
    components:{
        'navbar' : httpVueLoader('../components/navbar/navbar.vue'),
        'folder' : httpVueLoader('../components/folder/folder.vue'),
        'asset' : httpVueLoader('../components/asset/asset.vue'),
        'create-container-modal-window' : httpVueLoader('../components/create-container-modal-window/create-container-modal-window.vue'),
        'message' : httpVueLoader('../components/message/message.vue'),
        'asset-preview-modal-window' : httpVueLoader('../components/asset-preview-modal-window/asset-preview-modal-window.vue'),
        'asset-rename-modal-window' : httpVueLoader('../components/asset-rename-modal-window/asset-rename-modal-window.vue'),
        'asset-version-history-modal-window' : httpVueLoader('../components/asset-version-history-modal-window/asset-version-history-modal-window.vue'),
        'facet' : httpVueLoader('../components/facet/facet.vue'),
        'breadcrumb' : httpVueLoader('../components/breadcrumb/breadcrumb.vue'),
    }
});