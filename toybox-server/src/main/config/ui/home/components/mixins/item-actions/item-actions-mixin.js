var itemActionsMixin = {
    methods:{
        deleteItems(selectedItems){
            var selectionContext = this.generateSelectionContext(selectedItems);

            this.getService("toybox-common-object-loadbalancer")
            .then(response => {
                if(response){
                    return axios.post(response.data.value + '/common-objects/delete', selectionContext)
                        .then(response => {
                            console.log(response);
                            this.$root.$emit('message-sent', 'Success', response.data.message);
                            this.$root.$emit('refresh-assets');
                            this.$root.$emit('refresh-items');
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
                                this.$root.$emit('message-sent', 'Success', response.data.message);
                                this.$root.$emit('refresh-assets');
                                this.$root.$emit('refresh-items');
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
                            console.log(response);
                            if(response.status != 204){
                                this.$root.$emit('message-sent', 'Success', response.data.message);
                                this.$root.$emit('refresh-assets');
                                this.$root.$emit('refresh-items');
                            }
                            else{
                                this.$root.$emit('message-sent', 'Information', 'Selected assets were already unsubscribed.');
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
                }
            })
        },
        downloadItems(selectedItems){
            console.log(selectedItems);
            var selectionContext = this.generateSelectionContext(selectedItems);

            this.getService("toybox-common-object-loadbalancer")
            .then(response =>{
                if(response){
                    return axios.post(response.data.value + '/common-objects/download', selectionContext, {responseType:'blob'})
                        .then(response =>{
                            console.log(response);
                            var filename = 'Download.zip';
                            var blob = new Blob([response.data], {type:'application/octet-stream'});
                            saveAs(blob , filename);
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
                }
            });
        },
        shareItems(selectedItems){
            var selectionContext = this.generateSelectionContext(selectedItems);
            this.$root.$emit('open-share-modal-window', selectionContext);
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