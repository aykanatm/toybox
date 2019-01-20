const files = new Vue({
    el: '#toybox-files',
    mixins:[paginationMixin],
    data:{
        view: 'files',
        assets:[],
        selectedAssets: [],
        isLoading: false,
        // Sorting
        defaultSortType: 'des',
        defaultSortColumn: 'asset_name',
        sortType: 'des',
        sortColumn: 'asset_name',
        sortedAscByAssetName: false,
        sortedDesByAssetName: false,
        sortedAscByAssetImportDate: false,
        sortedDesByAssetImportDate: false,
        // Filtering
        facets: [],
        assetSearchRequestFacetList: [],
        // Messages
        messages: [],
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

        // Initialize accordions
        $('.ui.accordion').accordion();

        // Initialize event listeners
        this.$root.$on('perform-faceted-search', (facet, isAdd) => {
            if(isAdd){
                console.log('Adding facet ' + facet.fieldName + ' and its value ' + facet.fieldValue + ' to search');
                this.assetSearchRequestFacetList.push(facet);
            }
            else{
                console.log('Removing facet ' + facet.fieldName + ' and its value ' + facet.fieldValue + ' from search');
                var index;
                for(var i = 0; i < this.assetSearchRequestFacetList.length; i++){
                    var assetRequestFacet = this.assetSearchRequestFacetList[i];
                    if(assetRequestFacet.fieldName === facet.fieldName && assetRequestFacet.fieldValue === facet.fieldValue){
                        index = i;
                        break;
                    }
                }
                this.assetSearchRequestFacetList.splice(index, 1);
            }

            this.getAssets(this.offset, this.limit, this.sortType, this.sortColumn, this.username, this.assetSearchRequestFacetList);
        });
        this.$root.$on('asset-selection-changed', this.onAssetSelectionChanged);
        this.$root.$on('message-sent', this.displayMessage);

        this.getAssets(this.offset, this.limit, this.sortType, this.sortColumn, this.username, this.assetSearchRequestFacetList);
    },
    methods:{
        getConfiguration(fieldName){
            return axios.get("/configuration?field=" + fieldName)
                .catch(error => {
                    var errorMessage;

                    if(error.response){
                        errorMessage = error.response.data.message
                    }
                    else{
                        errorMessage = error.message;
                    }

                    console.error(errorMessage);
                    this.$root.$emit('message-sent', 'Error', errorMessage);
                });
        },
        getAssets(offset, limit, sortType, sortColumn, username, assetSearchRequestFacetList){
            this.isLoading = true;
            this.getConfiguration("assetServiceUrl")
            .then(response => {
                if(response){
                    var searchRequest = {};
                    searchRequest.limit = limit;
                    searchRequest.offset = offset;
                    searchRequest.sortType = sortType;
                    searchRequest.sortColumn = sortColumn;
                    searchRequest.username = username;
                    searchRequest.assetSearchRequestFacetList = assetSearchRequestFacetList;

                    return axios.post(response.data.value + "/assets/search", searchRequest)
                        .catch(error => {
                            var errorMessage;

                            if(error.response){
                                errorMessage = error.response.data.message
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
                    if(response.status != 204){
                        this.assets = response.data.assets;
                        this.facets = response.data.facets;
                        this.totalRecords = response.data.totalRecords;
                        this.totalPages = Math.ceil(this.totalRecords / this.limit);
                        this.currentPage = Math.ceil((offset / limit) + 1);
                    }
                    else{
                        this.displayMessage('Information','There is no asset in the system');
                    }
                }
            })
            .then(response => {
                console.log(response);
                this.updatePagination(this.currentPage, this.totalPages, offset, limit, this.totalRecords);
                // this.updateSortStatus(this.sortType, this.sortColumn)
            });
        },
        displayMessage(type, messageString){
            var isError = false;
            var isOk = false;
            var isWarning = false;
            var isInfo = false;

            if(type === 'Information'){
                isInfo = true;
            }
            else if(type === 'Error'){
                isError = true;
            }
            else if(type === 'Warning'){
                isWarning = true;
            }
            else if(type === 'Success'){
                isOk = true;
            }
            else{
                isInfo = true;
            }

            var message = {id:this.messages.length, isError:isError, isOk:isOk, isWarning:isWarning, isInfo:isInfo, header:type, message:messageString}
            this.messages.push(message);
            // Initialize events on the message
            setTimeout(() => {
                $('.message .close')
                    .on('click', function() {
                        $(this)
                        .closest('.message')
                        .remove();
                    });

                var messageSelector = '#' + message.id + '.message';
                $(messageSelector)
                    .delay(2000)
                    .queue(function(){
                        $(this).remove().dequeue();
                    });
            }, 100);
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
        share:function(){
            console.log('Sharing the following assets:');
            for(var i = 0; i < this.selectedAssets.length; i++)
            {
                var asset = this.selectedAssets[i];
                console.log(asset.assetName + '/ ' +  asset.assetId);
            }
        },
        download:function(){
            console.log('Downloading the following assets:');
            for(var i = 0; i < this.selectedAssets.length; i++)
            {
                var asset = this.selectedAssets[i];
                console.log(asset.assetName + '/ ' +  asset.assetId);
            }
        },
        copy:function(){
            console.log('Copying the following assets:');
            for(var i = 0; i < this.selectedAssets.length; i++)
            {
                var asset = this.selectedAssets[i];
                console.log(asset.assetName + '/ ' +  asset.assetId);
            }
        },
        move:function(){
            console.log('Moving the following assets:');
            for(var i = 0; i < this.selectedAssets.length; i++)
            {
                var asset = this.selectedAssets[i];
                console.log(asset.assetName + '/ ' +  asset.assetId);
            }
        },
        subscribe:function(){
            console.log('Subscribing to the following assets:');
            for(var i = 0; i < this.selectedAssets.length; i++)
            {
                var asset = this.selectedAssets[i];
                console.log(asset.assetName + '/ ' +  asset.assetId);
            }
        },
        delete:function(){
            console.log('Deleting the following assets:');
            for(var i = 0; i < this.selectedAssets.length; i++)
            {
                var asset = this.selectedAssets[i];
                console.log(asset.assetName + '/ ' +  asset.assetId);
            }
        },
    },
    components:{
        'navbar' : httpVueLoader('../components/navbar/navbar.vue'),
        'asset' : httpVueLoader('../components/asset/asset.vue'),
        'facet' : httpVueLoader('../components/facet/facet.vue'),
        'message' : httpVueLoader('../components/message/message.vue'),
    }
});