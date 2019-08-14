var assetActionsMixin = {
    methods:{
        deleteAssets(selectedAssets){
            this.getService("toybox-asset-loadbalancer")
                .then(response => {
                    if(response){
                        var assets = {
                            'selectedAssets': selectedAssets
                        }
                        return axios.post(response.data.value + '/assets/delete', assets)
                            .then(response => {
                                console.log(response);
                                this.$root.$emit('message-sent', 'Success', response.data.message);
                                this.$root.$emit('refresh-assets');
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
        subscribeToAssets(selectedAssets){
            this.getService("toybox-asset-loadbalancer")
                .then(response =>{
                    if(response){
                        var assets = {
                            'selectedAssets': selectedAssets
                        }

                        return axios.post(response.data.value + '/assets/subscribe', assets)
                            .then(response => {
                                console.log(response);
                                if(response.status != 204){
                                    this.$root.$emit('message-sent', 'Success', response.data.message);
                                    this.$root.$emit('refresh-assets');
                                    this.$root.$emit('refresh-items');
                                }
                                else{
                                    this.$root.$emit('message-sent', 'Information', 'Selected assets were already subscribed.');
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
        unsubscribeFromAssets(selectedAssets){
            this.getService("toybox-asset-loadbalancer")
            .then(response =>{
                if(response){
                    var assets = {
                        'selectedAssets': selectedAssets
                    }

                    return axios.post(response.data.value + '/assets/unsubscribe', assets)
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
        renameAsset(assetId, assetName){
            this.getService("toybox-asset-loadbalancer")
                .then(response => {
                    if(response){
                        this.$root.$emit('open-asset-rename-modal-window', assetId, assetName, response.data.value);
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
        showVersionHistory(assetId, thumbnailUrl){
            this.getService("toybox-asset-loadbalancer")
                .then(response => {
                    if(response){
                        this.$root.$emit('open-asset-version-history-modal-window', assetId, thumbnailUrl, response.data.value);
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
        moveAssets(selectedAssets){
            this.$root.$emit('open-copy-move-asset-modal-window', selectedAssets, true, false);
        },
        copyAssets(selectedAssets){
            this.$root.$emit('open-copy-move-asset-modal-window', selectedAssets, false, true);
        },
    }
}