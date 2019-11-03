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
                autofocus: false,
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

            $('.combo.dropdown')
            .dropdown({
                action: 'combo'
            });

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
            console.log('Opening share modal window for asset with ID "' + this.id + '"');
            var asset = {
                id: this.id,
                name: this.name,
                type: this.type,
                originalAssetId: this.originalAssetId,
                '@class': 'com.github.murataykanat.toybox.dbo.Asset',
                parentContainerId: this.parentContainerId,
                shared: this.shared,
                sharedByUsername: this.sharedByUsername
            }
            var selectedAssets = [asset];
            this.shareItems(selectedAssets);
            this.contextMenuOpen = false;
        },
        assetDownload:function(){
            console.log('Downloading the file with ID "' + this.id + '"');
            this.getService("toybox-rendition-loadbalancer")
                .then(response =>{
                    if(response){
                        return axios.get(response.data.value + '/renditions/assets/' + this.id + '/o', {responseType:'blob'})
                            .then(response =>{
                                console.log(response);
                                var filename = this.name;
                                var blob = new Blob([response.data], {type:'application/octet-stream'});
                                saveAs(blob , filename);

                                this.contextMenuOpen = false;
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

                                this.contextMenuOpen = false;
                            });
                    }
                    else{
                        this.$root.$emit('message-sent', 'Error', "There was no response from the service endpoint!");
                    }
                });
        },
        assetRename:function(){
            console.log('Renaming the file with ID "' + this.id + '"');
            var filename = this.name.substr(0, this.name.lastIndexOf('.')) || this.name;
            this.renameItem(this.id, filename, true);
            this.contextMenuOpen = false;
        },
        assetCopy:function(){
            console.log('Opening copy modal window for asset with ID "' + this.id + '"');
            var asset = {
                id: this.id,
                name: this.name,
                type: this.type,
                originalAssetId: this.originalAssetId,
                '@class': 'com.github.murataykanat.toybox.dbo.Asset',
                parentContainerId: this.parentContainerId,
                shared: this.shared,
                sharedByUsername: this.sharedByUsername
            }
            var selectedAssets= [asset];
            this.copyItems(selectedAssets);
            this.contextMenuOpen = false;
        },
        assetMove:function(){
            console.log('Opening move modal window for asset with ID "' + this.id + '"');
            var asset = {
                id: this.id,
                name: this.name,
                type: this.type,
                originalAssetId: this.originalAssetId,
                '@class': 'com.github.murataykanat.toybox.dbo.Asset',
                parentContainerId: this.parentContainerId,
                shared: this.shared,
                sharedByUsername: this.sharedByUsername
            }
            var selectedAssets= [asset];
            this.moveItems(selectedAssets);
            this.contextMenuOpen = false;
        },
        assetSubscribe:function(){
            console.log('Subscribing to the asset with ID "' + this.id + '"');
            this.contextMenuOpen = false;
            var asset = {
                id: this.id,
                name: this.name,
                type: this.type,
                originalAssetId: this.originalAssetId,
                '@class': 'com.github.murataykanat.toybox.dbo.Asset',
                parentContainerId: this.parentContainerId,
                shared: this.shared,
                sharedByUsername: this.sharedByUsername
            }
            var selectedAssets= [asset];
            this.subscribeToItems(selectedAssets);
            this.contextMenuOpen = false;
        },
        assetUnsubscribe:function(){
            console.log('Unsubscribing from the asset with ID "' + this.id + '"');
            this.contextMenuOpen = false;
            var asset = {
                id: this.id,
                name: this.name,
                type: this.type,
                originalAssetId: this.originalAssetId,
                '@class': 'com.github.murataykanat.toybox.dbo.Asset',
                parentContainerId: this.parentContainerId,
                shared: this.shared,
                sharedByUsername: this.sharedByUsername
            }
            var selectedAssets= [asset];
            this.unsubscribeFromItems(selectedAssets);
            this.contextMenuOpen = false;
        },
        assetDelete:function(){
            console.log('Deleting asset with ID "' + this.id + '"');
            var asset = {
                id: this.id,
                name: this.name,
                type: this.type,
                originalAssetId: this.originalAssetId,
                '@class': 'com.github.murataykanat.toybox.dbo.Asset',
                parentContainerId: this.parentContainerId,
                shared: this.shared,
                sharedByUsername: this.sharedByUsername
            }
            var selectedAssets= [asset];
            this.deleteItems(selectedAssets);
            this.contextMenuOpen = false;
        },
        assetShowVersionHistory:function(){
            console.log('Showing version history of asset with ID "' + this.id + '"');
            this.showVersionHistory(this.id, this.renditionUrl);
            this.contextMenuOpen = false;
        }
    }
}