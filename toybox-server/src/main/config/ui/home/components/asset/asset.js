module.exports = {
    mixins:[assetActionsMixin, serviceMixin, itemActionsMixin],
    props:{
        id: String,
        originalAssetId: String,
        name: String,
        importedByUsername: String,
        extension: String,
        renditionUrl: String,
        type: String,
        subscribed: String,
        parentContainerId: String
    },
    data: function() {
        return  {
          componentName: 'Asset',
          isSelected: false,
          hasThumbnail: true,
          contextMenuOpen: false,
          userAvatarUrl:'',
          thumbnailUrl:'',
          previewUrl:'',
          isImage: false,
          isVideo: false,
          isAudio: false,
          isPdf: false,
          isWord: false,
          isExcel: false,
          isPowerpoint: false,
          isDocument: false,
          isArchive: false
        }
    },
    watch:{
        isSelected:function(){
            this.$root.$emit('asset-selection-changed', this);
            this.$root.$emit('item-selection-changed', this);
        }
    },
    mounted:function(){
        this.userAvatarUrl = this.renditionUrl + '/renditions/users/' + this.importedByUsername;
        this.thumbnailUrl = this.renditionUrl + '/renditions/assets/' + this.id + '/t';
        this.previewUrl = this.renditionUrl + '/renditions/assets/' + this.id + '/p'
        this.isImage = this.type.startsWith('image') || this.type === 'application/postscript';
        this.isVideo = this.type.startsWith('video');
        this.isAudio = this.type.startsWith('audio');
        this.isPdf = this.type === 'application/pdf';
        this.isWord = this.type === 'application/msword' || this.type === 'application/vnd.openxmlformats-officedocument.wordprocessingml.document';
        this.isExcel = this.type === 'application/vnd.ms-excel' || this.type === 'application/vnd.openxmlformats-officedocument.spreadsheetml.sheet';
        this.isPowerpoint = this.type === 'application/vnd.ms-powerpoint' || this.type === 'application/vnd.openxmlformats-officedocument.presentationml.presentation';
        this.isDocument = this.isPdf || this.isWord || this.isExcel || this.isPowerpoint;
        this.isArchive = this.type === 'application/zip';

        this.$root.$on('display-asset-in-preview', (assetId) =>{
            if(this.id === assetId){
                this.$root.$emit('send-asset-to-preview', this);
            }
        });

        this.$root.$on('deselect-asset', this.assetDeselect);
        this.$root.$on('deselect-item', this.assetDeselect);
    },
    computed:{

    },
    methods:{
        onClick:function(){
            if(this.isSelected){
                this.isSelected = false;
            }
            else{
                this.isSelected = true;
            }
        },
        onRightClick:function(event){
            event.preventDefault();
            this.contextMenuOpen = true;
        },
        onMouseLeave:function(){
            this.contextMenuOpen = false;
        },
        onDoubleClick:function(){
            this.$root.$emit('open-asset-preview-modal-window', this);
        },
        onThumbnailImgSrcNotFound:function(){
            this.hasThumbnail = false;
        },
        assetDeselect:function(assetId){
            if(this.id === assetId){
                this.isSelected = false;
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
                parentContainerId: this.parentContainerId
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
                });
        },
        assetRename:function(){
            console.log('Renaming the file with ID "' + this.id + '"');
            var filename = this.name.substr(0, this.name.lastIndexOf('.')) || this.name;
            this.renameAsset(this.id, filename);
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
                parentContainerId: this.parentContainerId
            }
            var selectedAssets= [asset];
            this.copyAssets(selectedAssets);
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
                parentContainerId: this.parentContainerId
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
                parentContainerId: this.parentContainerId
            }
            var selectedAssets= [asset];
            this.subscribeToAssets(selectedAssets);
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
                parentContainerId: this.parentContainerId
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
                parentContainerId: this.parentContainerId
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