const folders = new Vue({
    el: '#toybox-folders',
    mixins:[paginationMixin, facetMixin, messageMixin, userMixin, serviceMixin, itemActionsMixin],
    data:{
        view: 'folders',
        items:[],
        breadcrumbs:[],
        selectedItems: [],
        isLoading: false,
        isDownloading: false,
        renditionUrl:'',
        canCreateFolder: false,
        canUploadFile: false,
        currentFolderId: '',
        // Searching
        searchQuery:'',
        // Sorting
        defaultSortType: 'DESC',
        defaultSortColumn: 'importDate',
        sortType: 'DESC',
        sortColumn: 'importDate',
        sortedAscByAssetName: false,
        sortedDesByAssetName: false,
        sortedAscByAssetImportDate: false,
        sortedDesByAssetImportDate: false,
        // Permissions
        canShare: false,
        canDownload: false,
        canCopy: false,
        canMove: false,
        canSubscribe: false,
        canUnsubscribe: false,
        canDelete: false,
        canEditCurrentContainer: false
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

        this.selectedItems = [];

        this.getService("toybox-rendition-loadbalancer")
            .then(response => {
                if(response){
                    this.renditionUrl = response.data.value;
                }
                else{
                    this.$root.$emit('message-sent', 'Error', "There was no response from the rendition loadbalancer!");
                }
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
        this.$root.$on('navigate-to-next-asset', this.onNavigateToNextAsset);
        this.$root.$on('navigate-to-previous-asset', this.onNavigateToPreviousAsset);
        this.$root.$on('update-arrows-request', this.updateArrows);
        this.$root.$on('refresh-items', this.refreshItems);
        this.$root.$on('open-folder', this.openFolder)
        this.$root.$on('perform-contextual-search', searchQuery => {
            this.searchQuery = searchQuery;

            this.getItems(this.currentFolderId, this.offset, this.limit, this.sortType, this.sortColumn, this.searchRequestFacetList)
        });

        setTimeout(() => {
            this.getItems(this.currentFolderId, this.offset, this.limit, this.sortType, this.sortColumn, this.searchRequestFacetList);
        }, 500);
    },
    watch:{
        selectedItems(after, before){
            this.canShare = true;
            this.canDownload = true;
            this.canCopy = true;
            this.canMove = true;
            this.canSubscribe = true;
            this.canUnsubscribe = true;
            this.canDelete = true;

            if(after.length != 0){
                for(var i = 0; i < after.length; i++){
                    var item = after[i];
                    if(item.canDownload === 'N'){
                        this.canDownload = false;
                        break;
                    }
                }

                for(var i = 0; i < after.length; i++){
                    var item = after[i];
                    if(item.canCopy === 'N'){
                        this.canCopy = false;
                        break;
                    }
                }

                for(var i = 0; i < after.length; i++){
                    var item = after[i];
                    if(item.canShare === 'N'){
                        this.canShare = false;
                        break;
                    }
                }

                for(var i = 0; i < after.length; i++){
                    var item = after[i];
                    if(item.shared === 'Y'){
                        this.canDelete = false;
                        this.canMove = false;
                        break;
                    }
                }

                for(var i = 0; i < after.length; i++){
                    var item = after[i];
                    this.canSubscribe = this.canSubscribe && item.subscribed === 'N';
                    this.canUnsubscribe = this.canUnsubscribe && item.subscribed === 'Y';
                }
            }
            else{
                this.canShare = false;
                this.canDownload = false;
                this.canCopy = false;
                this.canMove = false;
                this.canSubscribe = false;
                this.canUnsubscribe = false;
                this.canDelete = false;
            }
        },
        deep: true
    },
    methods:{
        refresh:function(){
            this.getItems(this.currentFolderId, this.offset, this.limit, this.sortType, this.sortColumn, this.searchRequestFacetList);
        },
        openFolder:function(folder){
            this.currentFolderId = folder.id;
            this.searchRequestFacetList = [];

            this.getItems(this.currentFolderId, this.offset, this.limit, this.sortType, this.sortColumn, this.searchRequestFacetList);
        },
        getItems:function(containerId, offset, limit, sortType, sortColumn, searchRequestFacetList){
            this.isLoading = true;

            if(containerId === undefined || containerId === '' || containerId === null){
                containerId = 'root';
            }

            this.getService("toybox-folder-loadbalancer")
                .then(response => {
                    if(response){
                        var searchRequest = {
                            limit: limit,
                            offset: offset,
                            sortType: sortType,
                            sortColumn: sortColumn,
                            assetSearchRequestFacetList: searchRequestFacetList,
                            searchConditions:[
                                {
                                    keyword: 'N',
                                    field:'deleted',
                                    operator: 'EQUALS',
                                    dataType: 'STRING',
                                    booleanOperator: 'AND'
                                }
                            ]
                        };

                        if(this.searchQuery !== undefined && this.searchQuery !== ''){
                            var searchQuerySearchCondition = {
                                keyword: this.searchQuery,
                                field:'name',
                                operator: 'CONTAINS',
                                dataType: 'STRING',
                                booleanOperator: 'AND'
                            }

                            searchRequest.searchConditions.push(searchQuerySearchCondition)
                        }

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
                    else{
                        this.isLoading = false;
                        this.$root.$emit('message-sent', 'Error', "There was no response from the service endpoint!");
                    }
                })
                .then(response => {
                    console.log(response);
                    if(response){
                        this.isLoading = false;
                        this.items = response.data.containerItems;
                        this.facets = response.data.facets;
                        this.canEditCurrentContainer = response.data.canEdit === 'Y';
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
                    else{
                        this.isLoading = false;
                        this.$root.$emit('message-sent', 'Error', "There was no response from the folder loadbalancer!");
                    }
                });
        },
        onItemSelectionChanged:function(item){
            if(item.isSelected){
                this.selectedItems.push(item);
            }
            else{
                var index = this.selectedItems.indexOf(item);
                this.selectedItems.splice(index, 1);
            }

            console.log(this.selectedItems);
        },
        onNavigateToNextAsset:function(item){
            if(item){
                var itemId = item.id;
                var index = this.items.map(function(a){
                    return a.id;
                }).indexOf(itemId);
                if(index < this.items.length - 1){
                    this.$root.$emit('display-asset-in-preview', this.items[index + 1].id);
                }

                this.updateArrows(this.items[index + 1]);
            }
        },
        onNavigateToPreviousAsset(item){
            if(item){
                var itemId = item.id;
                var index = this.items.map(function(a){
                    return a.id;
                }).indexOf(itemId);
                if(index > 0){
                    this.$root.$emit('display-asset-in-preview', this.items[index - 1].id);
                }

                this.updateArrows(this.items[index - 1]);
            }
        },
        updateArrows:function(item){
            if(item){
                var itemId = item.id;
                var index = this.items.map(function(a){
                    return a.id;
                }).indexOf(itemId);

                var canNavigateToNextAsset = false;
                var canNavigateToPreviousAsset = false;

                if(this.items.length > 1){
                    if(index > 0 && this.items[index + -1]['@class'] == 'com.github.murataykanat.toybox.dbo.Asset'){
                        canNavigateToPreviousAsset = true;
                    }
                    if(index < this.items.length - 1 && this.items[index + 1]['@class'] == 'com.github.murataykanat.toybox.dbo.Asset'){
                        canNavigateToNextAsset = true;
                    }
                }

                this.$root.$emit('update-arrows', canNavigateToNextAsset, canNavigateToPreviousAsset);
            }
        },
        itemsShare:function(){
            var selectedItems = this.getSelectedItems();
            this.shareItems(selectedItems);
        },
        itemsDownload:function(){
            var selectedItems = this.getSelectedItems();
            this.downloadItems(selectedItems);
        },
        itemsCopy:function(){
            var selectedItems = this.getSelectedItems();
            this.copyItems(selectedItems);
        },
        itemsMove:function(){
            var selectedItems = this.getSelectedItems();
            this.moveItems(selectedItems);
        },
        itemsSubscribe:function(){
            var selectedItems = this.getSelectedItems();
            this.subscribeToItems(selectedItems);
        },
        itemsUnsubscribe:function(){
            var selectedItems = this.getSelectedItems();
            this.unsubscribeFromItems(selectedItems);
        },
        itemsDelete:function(){
            var selectedItems = this.getSelectedItems();
            this.deleteItems(selectedItems);
        },
        refreshItems:function(){
            this.selectedItems = [];
            for(var i = 0; i < this.items.length; i++){
                var item = this.items[i];
                this.$root.$emit('deselect-item', item.id);
            }

            this.getItems(this.currentFolderId, this.offset, this.limit, this.sortType, this.sortColumn, this.searchRequestFacetList);
        },
        createNewFolder:function(){
            this.getService("toybox-folder-loadbalancer")
            .then(response => {
                if(response){
                    this.$root.$emit('open-create-container-modal-window', this.currentFolderId, response.data.value);
                }
                else{
                    this.$root.$emit('message-sent', 'Error', "There was no response from the service endpoint!");
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
                var itemClass = 'com.github.murataykanat.toybox.dbo.Asset';
                if(item.type === undefined){
                    itemClass = 'com.github.murataykanat.toybox.dbo.Container';
                }

                selectedItems.push({
                    id:item.id,
                    name:item.name,
                    type:item.type,
                    originalAssetId:item.originalAssetId,
                    '@class': itemClass,
                    parentContainerId: item.parentContainerId,
                    shared: item.shared,
                    sharedByUsername: item.sharedByUsername,
                    canDownload: item.canDownload,
                    canCopy: item.canCopy,
                    canEdit: item.canEdit,
                    canShare: item.canShare
                });
            }

            return selectedItems;
        },
        updateButtons:function(){
            if(this.currentFolderId && this.currentFolderId !== '' && this.canEditCurrentContainer){
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
        'copy-move-asset-modal-window': httpVueLoader('../components/copy-move-asset-modal-window/copy-move-asset-modal-window.vue'),
        'share-modal-window': httpVueLoader('../components/share-modal-window/share-modal-window.vue'),
        'facet' : httpVueLoader('../components/facet/facet.vue'),
        'breadcrumb' : httpVueLoader('../components/breadcrumb/breadcrumb.vue'),
    }
});