module.exports = {
    data:function(){
        return{
            componentName: 'Rename Modal Window',
            id:'',
            name:'',
            serviceUrl: '',
            isAsset: true
        }
    },
    mounted:function(){
        this.$root.$on('open-asset-rename-modal-window', (id, name, serviceUrl, isAsset) => {
            this.id = id;
            this.name = name;
            this.serviceUrl = serviceUrl;
            this.isAsset = isAsset;

            if(isAsset){
                this.serviceUrl += '/assets/';
            }
            else{
                this.serviceUrl += '/containers/';
            }

            $(this.$el).modal('setting', 'closable', false).modal('show');
        });
    },
    methods:{
        renameItem:function(){
            var updateAssetRequest = {
                'name': this.name
            }

            axios.patch(this.serviceUrl + this.id, updateAssetRequest)
                .then(response => {
                    this.$root.$emit('message-sent', 'Success', response.data.message);
                    this.$root.$emit('refresh-assets');
                    this.$root.$emit('refresh-items');
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