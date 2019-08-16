var assetActionsMixin = {
    methods:{
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