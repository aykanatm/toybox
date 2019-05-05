module.exports = {
    data:function(){
        return{
            componentName: 'Rename Modal Window',
            assetId:'',
            assetName:'',
            assetUrl: ''
        }
    },
    mounted:function(){
        this.$root.$on('open-asset-rename-modal-window', (assetId, assetName, assetUrl) => {
            this.assetId = assetId;
            this.assetName = assetName;
            this.assetUrl = assetUrl;

            $(this.$el).modal('setting', 'closable', false).modal('show');
        });
    },
    methods:{
        renameAsset:function(){
            var updateAssetRequest = {
                'name': this.assetName
            }

            axios.patch(this.assetUrl + '/assets/' + this.assetId, updateAssetRequest)
                .then(response => {
                    this.$root.$emit('message-sent', 'Success', response.data.message);
                    this.$root.$emit('refresh-assets');
                    $(this.$el).modal('hide');
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