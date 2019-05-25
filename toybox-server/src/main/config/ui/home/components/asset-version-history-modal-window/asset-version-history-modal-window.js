module.exports = {
    data:function(){
        return{
            componentName: 'Rename Modal Window',
            assetId:'',
            assetUrl:'',
            versionHistoryAssets:[],
            renditionUrl:''
        }
    },
    mounted:function(){
        this.$root.$on('refresh-asset-version-history', this.retrieveVersionHistory);
        this.$root.$on('open-asset-version-history-modal-window', (assetId, renditionUrl, assetUrl) => {
            this.assetId = assetId;
            this.assetUrl = assetUrl;
            this.renditionUrl = renditionUrl;

            $(this.$el).modal('setting', 'closable', false).modal('show');
            this.retrieveVersionHistory();
        });
    },
    methods:{
        retrieveVersionHistory(){
            axios.get(this.assetUrl + '/assets/' + this.assetId + '/versions')
                .then(response => {
                    console.log(response.data);
                    this.versionHistoryAssets = response.data.assets;
                    this.$root.$emit('message-sent', 'Success', response.data.message);
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
    },
    components:{
        'asset-version-history' : httpVueLoader('../asset-version-history/asset-version-history.vue')
    }
}