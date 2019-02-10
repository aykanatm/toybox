module.exports = {
    props:{
        renditionUrl: String
    },
    data: function() {
        return  {
          componentName: 'Asset Preview Modal Window',
          asset: Object,
          hasPreview: true,
          canNavigateToNextAsset: false,
          canNavigateToPreviousAsset: false,
        }
    },
    mounted:function(){
        this.$root.$on('open-asset-preview-modal-window', (asset) => {
            console.log(asset);
            this.asset = asset;
            $(this.$el).modal('setting', 'closable', false).modal({
                onVisible: function(){
                    if($('video').length){
                        $('video')[0].load();
                    }
                    if($('audio').length){
                        $('audio')[0].load();
                    }
                },
                onHide: function(){
                    if($('video').length){
                        $('video')[0].pause();
                    }
                    if($('audio').length){
                        $('audio')[0].pause();
                    }
                }
            }).modal('show');

            this.$root.$emit('update-arrows-request', this.asset);
        });

        this.$root.$on('send-asset-to-preview', (asset) =>{
            this.asset = asset;
        });

        this.$root.$on('update-arrows', this.onUpdateArrows);
    },
    watch:{

    },
    computed:{

    },
    methods:{
        onUpdateArrows:function(canNavigateToNextAsset, canNavigateToPreviousAsset){
            this.canNavigateToNextAsset = canNavigateToNextAsset;
            this.canNavigateToPreviousAsset = canNavigateToPreviousAsset;
        },
        nextAsset:function(){
            if(this.canNavigateToNextAsset){
                this.$root.$emit('navigate-to-next-asset', this.asset);
            }
        },
        previousAsset:function(){
            if(this.canNavigateToPreviousAsset){
                this.$root.$emit('navigate-to-previous-asset', this.asset);
            }
        },
        assetShare:function(){
            console.log('Opening share modal window for asset with ID "' + this.asset.id + '"');
        },
        assetDownload:function(){
            console.log('Downloading the file with ID "' + this.asset.id + '"');
        },
        assetRename:function(){
            console.log('Renaming the file with ID "' + this.asset.id + '"');
        },
        assetCopy:function(){
            console.log('Opening copy modal window for asset with ID "' + this.asset.id + '"');
        },
        assetMove:function(){
            console.log('Opening move modal window for asset with ID "' + this.asset.id + '"');
        },
        assetSubscribe:function(){
            console.log('Subscribing to the asset with ID "' + this.asset.id + '"');
        },
        assetDelete:function(){
            console.log('Deleting asset with ID "' + this.asset.id + '"');
        },
        assetShowVersionHistory:function(){
            console.log('Showing version history of asset with ID "' + this.asset.id + '"');
        }
    }
}