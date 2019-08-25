var assetActionsMixin = {
    methods:{
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
        }
    }
}