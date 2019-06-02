const folders = new Vue({
    el: '#toybox-folders',
    mixins:[paginationMixin, facetMixin, messageMixin, userMixin, serviceMixin, itemActionsMixin],
    data:{
        view: 'folders',
        facets:[],
        items:[],
        selectedItems: [],
        isLoading: false,
        renditionUrl:'',
        canCreateFolder: false,
        canUploadFile: false,
        currentFolderId: ''
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

        this.$root.$on('item-selection-changed', this.onItemSelectionChanged);
        this.$root.$on('message-sent', this.displayMessage);
        this.$root.$on('refresh-items', this.refreshItems);

        this.getTopLevelFolders(this.offset, this.limit);
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

                    this.currentFolderId = '';
                    this.updateButtons();

                    if(response.status == 204){
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

                    this.updatePagination(this.currentPage, this.totalPages, offset, limit, this.totalRecords);
                }
            });
        },
        getItems:function(offset, limit, sortType, sortColumn, searchRequestFacetList){

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
                    this.$root.$emit('open-create-container-modal-window', this.parentContainerId, response.data.value);
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
            if(this.currentFolderId !== ''){
                this.canCreateFolder = true;
                this.canUploadFile = true;
            }
            else{
                if(this.user.isAdmin){
                    this.canCreateFolder = true;
                    this.canUploadFile = true;
                }
                else{
                    this.canCreateFolder = false;
                    this.canUploadFile = false;
                }
            }
        }
    },
    components:{
        'navbar' : httpVueLoader('../components/navbar/navbar.vue'),
        'folder' : httpVueLoader('../components/folder/folder.vue'),
        'create-container-modal-window' : httpVueLoader('../components/create-container-modal-window/create-container-modal-window.vue'),
        'message' : httpVueLoader('../components/message/message.vue')
    }
});