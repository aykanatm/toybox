const files = new Vue({
    el: '#toybox-files',
    mixins:[paginationMixin, messageMixin, facetMixin, userMixin, assetActionsMixin, itemActionsMixin, serviceMixin],
    data:{
        view: 'files',
        assets:[],
        selectedAssets: [],
        isLoading: false,
        isDownloading: false,
        // Sorting
        defaultSortType: 'des',
        defaultSortColumn: 'asset_import_date',
        sortType: 'des',
        sortColumn: 'asset_import_date',
        sortedAscByAssetName: false,
        sortedDesByAssetName: false,
        sortedAscByAssetImportDate: false,
        sortedDesByAssetImportDate: false,
        // Services
        renditionUrl: '',
        // Permissions
        canShare: false,
        canDownload: false,
        canCopy: false,
        canMove: false,
        canSubscribe: false,
        canUnsubscribe: false,
        canDelete: false,
        searchQuery:'',
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

        this.selectedAssets = [];

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

            this.getAssets(this.offset, this.limit, this.sortType, this.sortColumn, this.searchRequestFacetList);
        });
        this.$root.$on('asset-selection-changed', this.onAssetSelectionChanged);
        this.$root.$on('message-sent', this.displayMessage);
        this.$root.$on('navigate-to-next-asset', this.onNavigateToNextAsset);
        this.$root.$on('navigate-to-previous-asset', this.onNavigateToPreviousAsset);
        this.$root.$on('update-arrows-request', this.updateArrows);
        this.$root.$on('refresh-assets', this.refreshAssets);
        this.$root.$on('perform-contextual-search', searchQuery => {
            this.selectedAssets = [];
            if(this.assets !== undefined){
                for(var i = 0; i < this.assets.length; i++){
                    var asset = this.assets[i];
                    this.$root.$emit('deselect-asset', asset.id);
                }
            }

            this.searchQuery = searchQuery;

            this.getAssets(this.offset, this.limit, this.sortType, this.sortColumn, this.searchRequestFacetList);
        });

        this.getAssets(this.offset, this.limit, this.sortType, this.sortColumn, this.searchRequestFacetList);
    },
    watch:{
        selectedAssets(after, before){
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
        getAssets(offset, limit, sortType, sortColumn, searchRequestFacetList){
            this.isLoading = true;
            this.getService("toybox-asset-loadbalancer")
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

                    return axios.post(response.data.value + "/assets/search", searchRequest)
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

                    this.assets = response.data.assets;
                    this.facets = response.data.facets;

                    if(response.status == 204){
                        this.displayMessage('Information','You do not have any files.');
                        this.totalRecords = 0;
                        this.totalPages = 0;
                        this.currentPage = 0;
                    }
                    else{
                        this.totalRecords = response.data.totalRecords;
                        this.totalPages = Math.ceil(this.totalRecords / this.limit);
                        this.currentPage = Math.ceil((offset / limit) + 1);
                    }

                    this.updatePagination(this.currentPage, this.totalPages, offset, limit, this.totalRecords);
                }
                else{
                    this.isLoading = false;
                    this.$root.$emit('message-sent', 'Error', "There was no response from the aseet loadbalancer!");
                }
            });
        },
        onAssetSelectionChanged:function(asset){
            if(asset.isSelected){
                this.selectedAssets.push(asset);
            }
            else{
                var index = this.selectedAssets.indexOf(asset);
                this.selectedAssets.splice(index, 1);
            }

            console.log(this.selectedAssets);
        },
        onNavigateToNextAsset:function(asset){
            if(asset){
                var assetId = asset.id;
                var index = this.assets.map(function(a){
                    return a.id;
                }).indexOf(assetId);
                if(index < this.assets.length - 1){
                    this.$root.$emit('display-asset-in-preview', this.assets[index + 1].id);
                }

                this.updateArrows(this.assets[index + 1]);
            }
        },
        onNavigateToPreviousAsset(asset){
            if(asset){
                var assetId = asset.id;
                var index = this.assets.map(function(a){
                    return a.id;
                }).indexOf(assetId);
                if(index > 0){
                    this.$root.$emit('display-asset-in-preview', this.assets[index - 1].id);
                }

                this.updateArrows(this.assets[index - 1]);
            }
        },
        updateArrows:function(asset){
            if(asset){
                var assetId = asset.id;
                var index = this.assets.map(function(a){
                    return a.id;
                }).indexOf(assetId);

                var canNavigateToNextAsset = false;
                var canNavigateToPreviousAsset = false;

                if(this.assets.length > 1){
                    if(index > 0){
                        canNavigateToPreviousAsset = true;
                    }
                    if(index < this.assets.length - 1){
                        canNavigateToNextAsset = true;
                    }
                }

                this.$root.$emit('update-arrows', canNavigateToNextAsset, canNavigateToPreviousAsset);
            }
        },
        assetsShare:function(){
            var selectedAssets = this.getSelectedAssets();
            this.shareItems(selectedAssets);
        },
        assetsDownload:function(){
            var selectedAssets = this.getSelectedAssets();
            this.downloadItems(selectedAssets);
        },
        assetsCopy:function(){
            var selectedAssets = this.getSelectedAssets();
            this.copyItems(selectedAssets);
        },
        assetsMove:function(){
            var selectedAssets = this.getSelectedAssets();
            this.moveItems(selectedAssets);
        },
        assetsSubscribe:function(){
            var selectedAssets = this.getSelectedAssets();
            this.subscribeToItems(selectedAssets);
        },
        assetsUnsubscribe:function(){
            var selectedAssets = this.getSelectedAssets();
            this.unsubscribeFromItems(selectedAssets);
        },
        assetsDelete:function(){
            var selectedAssets = this.getSelectedAssets();
            this.deleteItems(selectedAssets);
        },
        refreshAssets:function(){
            this.selectedAssets = [];
            if(this.assets !== undefined){
                for(var i = 0; i < this.assets.length; i++){
                    var asset = this.assets[i];
                    this.$root.$emit('deselect-asset', asset.id);
                }
            }

            this.$root.$emit('clear-search-query');
            this.searchQuery = '';
            this.getAssets(this.offset, this.limit, this.sortType, this.sortColumn, this.searchRequestFacetList);
        },
        getSelectedAssets:function(){
            var selectedAssets = []
            for(var i = 0; i < this.selectedAssets.length; i++)
            {
                var asset = this.selectedAssets[i];
                selectedAssets.push({
                    id:asset.id,
                    name:asset.name,
                    type:asset.type,
                    originalAssetId:asset.originalAssetId,
                    '@class': 'com.github.murataykanat.toybox.dbo.Asset',
                    parentContainerId: asset.parentContainerId,
                    shared: asset.shared,
                    sharedByUsername: asset.sharedByUsername,
                    canDownload: asset.canDownload,
                    canCopy: asset.canCopy,
                    canEdit: asset.canEdit,
                    canShare: asset.canShare
                });
            }

            return selectedAssets;
        }
    },
    components:{
        'navbar': httpVueLoader('../components/navbar/navbar.vue'),
        'asset': httpVueLoader('../components/asset/asset.vue'),
        'asset-preview-modal-window': httpVueLoader('../components/asset-preview-modal-window/asset-preview-modal-window.vue'),
        'asset-rename-modal-window': httpVueLoader('../components/asset-rename-modal-window/asset-rename-modal-window.vue'),
        'asset-version-history-modal-window': httpVueLoader('../components/asset-version-history-modal-window/asset-version-history-modal-window.vue'),
        'share-modal-window': httpVueLoader('../components/share-modal-window/share-modal-window.vue'),
        'copy-move-asset-modal-window': httpVueLoader('../components/copy-move-asset-modal-window/copy-move-asset-modal-window.vue'),
        'facet': httpVueLoader('../components/facet/facet.vue'),
        'message': httpVueLoader('../components/message/message.vue'),
    }
});