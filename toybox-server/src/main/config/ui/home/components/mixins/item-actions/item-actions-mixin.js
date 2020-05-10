var itemActionsMixin = {
    methods:{
        deleteItems(selectedItems){
            var selectionContext = this.generateSelectionContext(selectedItems);

            this.getService("toybox-common-object-loadbalancer")
            .then(response => {
                if(response){
                    return axios.post(response.data.value + '/common-objects/delete', selectionContext)
                        .then(response => {
                            if(response){
                                console.log(response);
                                this.$root.$emit('message-sent', 'Success', response.data.message);
                                this.$root.$emit('refresh-assets');
                                this.$root.$emit('refresh-items');
                            }
                            else{
                                this.$root.$emit('message-sent', 'Error', "There was no response from the common object loadbalancer!");
                            }
                        })
                        .catch(error => {
                            var errorMessage;

                            if(error.response){
                                errorMessage = error.response.data.message
                                if(error.response.status == 401){
                                    window.location = '/logout';
                                }
                                else if(error.response.status == 403){
                                    this.$root.$emit('message-sent', 'Warning', errorMessage);
                                }
                                else{
                                    console.error(errorMessage);
                                    this.$root.$emit('message-sent', 'Error', errorMessage);
                                }
                            }
                            else{
                                errorMessage = error.message;

                                console.error(errorMessage);
                                this.$root.$emit('message-sent', 'Error', errorMessage);
                            }
                        });
                }
                else{
                    this.$root.$emit('message-sent', 'Error', "There was no response from the service endpoint!");
                }
            })
        },
        subscribeToItems(selectedItems){
            var selectionContext = this.generateSelectionContext(selectedItems);

            this.getService("toybox-common-object-loadbalancer")
                .then(response =>{
                    if(response){
                        return axios.post(response.data.value + '/common-objects/subscribe', selectionContext)
                            .then(response => {
                                if(response){
                                    this.$root.$emit('message-sent', 'Success', response.data.message);
                                    this.$root.$emit('refresh-assets');
                                    this.$root.$emit('refresh-items');
                                }
                                else{
                                    this.$root.$emit('message-sent', 'Error', "There was no response from the common object loadbalancer!");
                                }
                            })
                            .catch(error => {
                                var errorMessage;

                                if(error.response){
                                    errorMessage = error.response.data.message
                                    if(error.response.status == 401){
                                        window.location = '/logout';
                                    }
                                    else if(error.response.status == 403){
                                        this.$root.$emit('message-sent', 'Warning', errorMessage);
                                    }
                                    else{
                                        console.error(errorMessage);
                                        this.$root.$emit('message-sent', 'Error', errorMessage);
                                    }
                                }
                                else{
                                    errorMessage = error.message;

                                    console.error(errorMessage);
                                    this.$root.$emit('message-sent', 'Error', errorMessage);
                                }
                            });
                    }
                    else{
                        this.$root.$emit('message-sent', 'Error', "There was no response from the service endpoint!");
                    }
                })
        },
        unsubscribeFromItems(selectedItems){
            var selectionContext = this.generateSelectionContext(selectedItems);

            this.getService("toybox-common-object-loadbalancer")
            .then(response =>{
                if(response){
                    return axios.post(response.data.value + '/common-objects/unsubscribe', selectionContext)
                        .then(response => {
                            if(response){
                                console.log(response);
                                if(response.status != 204){
                                    this.$root.$emit('message-sent', 'Success', response.data.message);
                                    this.$root.$emit('refresh-assets');
                                    this.$root.$emit('refresh-items');
                                }
                                else{
                                    this.$root.$emit('message-sent', 'Information', 'Selected assets were already unsubscribed.');
                                }
                            }
                            else{
                                this.$root.$emit('message-sent', 'Error', "There was no response from the common object loadbalancer!");
                            }
                        })
                        .catch(error => {
                            var errorMessage;

                            if(error.response){
                                errorMessage = error.response.data.message
                                if(error.response.status == 401){
                                    window.location = '/logout';
                                }
                                else if(error.response.status == 403){
                                    this.$root.$emit('message-sent', 'Warning', errorMessage);
                                }
                                else{
                                    console.error(errorMessage);
                                    this.$root.$emit('message-sent', 'Error', errorMessage);
                                }
                            }
                            else{
                                errorMessage = error.message;

                                console.error(errorMessage);
                                this.$root.$emit('message-sent', 'Error', errorMessage);
                            }
                        });
                }
                else{
                    this.$root.$emit('message-sent', 'Error', "There was no response from the service endpoint!");
                }
            });
        },
        downloadItems(selectedItems){
            this.isDownloading = true;

            console.log(selectedItems);
            var selectionContext = this.generateSelectionContext(selectedItems);

            this.getService("toybox-common-object-loadbalancer")
            .then(response =>{
                if(response){
                    return axios.post(response.data.value + '/common-objects/download', selectionContext, {responseType:'blob'})
                        .then(response =>{
                            if(response){
                                console.log(response);
                                var filename = 'Download.zip';
                                var blob = new Blob([response.data], {type:'application/octet-stream'});
                                saveAs(blob , filename);

                                this.isDownloading = false;
                            }
                            else{
                                this.$root.$emit('message-sent', 'Error', "There was no response from the common object loadbalancer!");
                            }
                        })
                        .catch(error => {
                            this.isDownloading = false;

                            var errorMessage;

                            if(error.response){
                                errorMessage = error.response.data.message
                                if(error.response.status == 401){
                                    window.location = '/logout';
                                }
                                else if(error.response.status == 403){
                                    this.$root.$emit('message-sent', 'Warning', errorMessage);
                                }
                                else{
                                    console.error(errorMessage);
                                    this.$root.$emit('message-sent', 'Error', errorMessage);
                                }
                            }
                            else{
                                errorMessage = error.message;

                                console.error(errorMessage);
                                this.$root.$emit('message-sent', 'Error', errorMessage);
                            }
                        });
                }
                else{
                    this.isDownloading = false;
                    this.$root.$emit('message-sent', 'Error', "There was no response from the service endpoint!");
                }
            });
        },
        shareItems(selectedItems){
            var selectionContext = this.generateSelectionContext(selectedItems);
            this.$root.$emit('open-share-modal-window', selectionContext, undefined, undefined);
        },
        moveItems(selectedItems){
            var selectionContext = this.generateSelectionContext(selectedItems);
            this.$root.$emit('open-copy-move-asset-modal-window', selectionContext, true, false);
        },
        copyItems(selectedItems){
            var selectionContext = this.generateSelectionContext(selectedItems);
            this.$root.$emit('open-copy-move-asset-modal-window', selectionContext, false, true);
        },
        restoreItems(selectedItems){
            var selectionContext = this.generateSelectionContext(selectedItems);

            this.getService("toybox-common-object-loadbalancer")
            .then(response =>{
                if(response){
                    return axios.post(response.data.value + '/common-objects/restore', selectionContext)
                        .then(response => {
                            if(response){
                                console.log(response);
                                this.$root.$emit('message-sent', 'Success', response.data.message);
                                this.$root.$emit('refresh-assets');
                                this.$root.$emit('refresh-items');
                            }
                            else{
                                this.$root.$emit('message-sent', 'Error', "There was no response from the common object loadbalancer!");
                            }
                        })
                        .catch(error => {
                            var errorMessage;

                            if(error.response){
                                errorMessage = error.response.data.message
                                if(error.response.status == 401){
                                    window.location = '/logout';
                                }
                                else if(error.response.status == 403){
                                    this.$root.$emit('message-sent', 'Warning', errorMessage);
                                }
                                else{
                                    console.error(errorMessage);
                                    this.$root.$emit('message-sent', 'Error', errorMessage);
                                }
                            }
                            else{
                                errorMessage = error.message;

                                console.error(errorMessage);
                                this.$root.$emit('message-sent', 'Error', errorMessage);
                            }
                        });
                }
                else{
                    this.$root.$emit('message-sent', 'Error', "There was no response from the service endpoint!");
                }
            });
        },
        purgeItems(selectedItems){
            var selectionContext = this.generateSelectionContext(selectedItems);

            this.getService("toybox-common-object-loadbalancer")
            .then(response =>{
                if(response){
                    return axios.post(response.data.value + '/common-objects/purge', selectionContext)
                        .then(response => {
                            if(response){
                                console.log(response);
                                this.$root.$emit('message-sent', 'Success', response.data.message);
                                this.$root.$emit('refresh-assets');
                                this.$root.$emit('refresh-items');
                            }
                            else{
                                this.$root.$emit('message-sent', 'Error', "There was no response from the common object loadbalancer!");
                            }
                        })
                        .catch(error => {
                            var errorMessage;

                            if(error.response){
                                errorMessage = error.response.data.message
                                if(error.response.status == 401){
                                    window.location = '/logout';
                                }
                                else if(error.response.status == 403){
                                    this.$root.$emit('message-sent', 'Warning', errorMessage);
                                }
                                else{
                                    console.error(errorMessage);
                                    this.$root.$emit('message-sent', 'Error', errorMessage);
                                }
                            }
                            else{
                                errorMessage = error.message;

                                console.error(errorMessage);
                                this.$root.$emit('message-sent', 'Error', errorMessage);
                            }
                        });
                }
                else{
                    this.$root.$emit('message-sent', 'Error', "There was no response from the service endpoint!");
                }
            });
        },
        renameItem(id, name, isAsset){
            var serviceName;
            if(isAsset){
                serviceName = "toybox-asset-loadbalancer";
            }
            else{
                serviceName = "toybox-folder-loadbalancer";
            }
            this.getService(serviceName)
            .then(response => {
                if(response){
                    this.$root.$emit('open-asset-rename-modal-window', id, name, response.data.value, isAsset);
                }
            })
            .catch(error => {
                var errorMessage;

                if(error.response){
                    errorMessage = error.response.data.message
                    if(error.response.status == 401){
                        window.location = '/logout';
                    }
                    else if(error.response.status == 403){
                        this.$root.$emit('message-sent', 'Warning', errorMessage);
                    }
                    else{
                        console.error(errorMessage);
                        this.$root.$emit('message-sent', 'Error', errorMessage);
                    }
                }
                else{
                    errorMessage = error.message;

                    console.error(errorMessage);
                    this.$root.$emit('message-sent', 'Error', errorMessage);
                }
            });
        },
        generateSelectionContext(selectedItems){
            console.log(selectedItems);
            var selectedAssets = [];
            var selectedContainers = [];

            for(var i = 0; i < selectedItems.length; i++){
                var selectedItem = selectedItems[i];
                if(selectedItem['@class'] === 'com.github.murataykanat.toybox.dbo.Asset'){
                    selectedAssets.push(selectedItem);
                }
                else{
                    selectedContainers.push(selectedItem);
                }
            }

            var selectionContext = {
                'selectedAssets': selectedAssets,
                'selectedContainers': selectedContainers
            }

            return selectionContext;
        }
    }
}