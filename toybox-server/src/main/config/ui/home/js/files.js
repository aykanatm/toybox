const files = new Vue({
    el: '#toybox-files',
    mixins:[paginationMixin, messageMixin, facetMixin, userMixin, assetActionsMixin, itemActionsMixin, serviceMixin],
    data:{
        view: 'files',
        assets:[],
        selectedAssets: [],
        isLoading: false,
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
        renditionUrl: ''
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

            // TODO: username is not necessary?
            this.getAssets(this.offset, this.limit, this.sortType, this.sortColumn, this.username, this.searchRequestFacetList);
        });
        this.$root.$on('asset-selection-changed', this.onAssetSelectionChanged);
        this.$root.$on('message-sent', this.displayMessage);
        this.$root.$on('navigate-to-next-asset', this.onNavigateToNextAsset);
        this.$root.$on('navigate-to-previous-asset', this.onNavigateToPreviousAsset);
        this.$root.$on('update-arrows-request', this.updateArrows);
        this.$root.$on('refresh-assets', this.refreshAssets);

        this.getAssets(this.offset, this.limit, this.sortType, this.sortColumn, this.username, this.searchRequestFacetList);
    },
    methods:{
        getAssets(offset, limit, sortType, sortColumn, username, searchRequestFacetList){
            this.isLoading = true;
            this.getService("toybox-asset-loadbalancer")
            .then(response => {
                if(response){
                    var searchRequest = {};
                    searchRequest.limit = limit;
                    searchRequest.offset = offset;
                    searchRequest.sortType = sortType;
                    searchRequest.sortColumn = sortColumn;
                    searchRequest.username = username;
                    searchRequest.assetSearchRequestFacetList = searchRequestFacetList;

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
            });
        },
        onAssetSelectionChanged:function(asset){
            if(asset.isSelected){
                this.selectedAssets.push(asset);
            }
            else{
                this.selectedAssets.splice(asset, 1);
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
            this.copyAssets(selectedAssets);
        },
        assetsMove:function(){
            var selectedAssets = this.getSelectedAssets();
            this.moveAssets(selectedAssets);
        },
        assetsSubscribe:function(){
            var selectedAssets = this.getSelectedAssets();
            this.subscribeToItems(selectedAssets);
        },
        assetsUnsubscribe:function(){
            var selectedAssets = this.getSelectedAssets();
            this.unsubscribeFromAssets(selectedAssets);
        },
        assetsDelete:function(){
            var selectedAssets = this.getSelectedAssets();
            this.deleteItems(selectedAssets);
        },
        refreshAssets:function(){
            this.selectedAssets = [];
            for(var i = 0; i < this.assets.length; i++){
                var asset = this.assets[i];
                this.$root.$emit('deselect-asset', asset.id);
            }

            this.getAssets(this.offset, this.limit, this.sortType, this.sortColumn, this.username, this.searchRequestFacetList);
        },
        getSelectedAssets:function(){
            var selectedAssets = []
            for(var i = 0; i < this.selectedAssets.length; i++)
            {
                var asset = this.selectedAssets[i];
                selectedAssets.push({id:asset.id, name:asset.name, type:asset.type, originalAssetId:asset.originalAssetId, '@class': 'com.github.murataykanat.toybox.dbo.Asset', parentContainerId: asset.parentContainerId});
            }

            return selectedAssets;
        },
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