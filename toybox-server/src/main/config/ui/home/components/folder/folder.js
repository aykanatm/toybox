module.exports = {
    mixins:[itemActionsMixin, serviceMixin],
    props:{
        id: String,
        name: String,
        createdByUsername: String,
        subscribed: String,
        renditionUrl: String
    },
    data: function() {
        return  {
          componentName: 'Folder',
          isSelected: false,
          contextMenuOpen: false,
          userAvatarUrl:'',
        }
    },
    watch:{
        isSelected:function(){
            this.$root.$emit('item-selection-changed', this);
        }
    },
    mounted:function(){
        this.userAvatarUrl = this.renditionUrl + '/renditions/users/' + this.createdByUsername;
        this.$root.$on('deselect-item', this.folderDeselect);
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
            this.$root.$emit('open-folder', this);
        },
        folderDeselect:function(folderId){
            if(this.id === assetId){
                this.isSelected = false;
            }
        },
        folderShare:function(){
            console.log('Opening share modal window for folder with ID "' + this.id + '"');
            this.contextMenuOpen = false;
        },
        folderDownload:function(){
            console.log('Downloading the folder with ID "' + this.id + '"');
            // var asset = {
            //     id: this.id,
            //     name: this.name,
            //     type: this.type,
            //     originalAssetId: this.originalAssetId
            // }
            // var selectedAssets= [asset];
            // this.downloadAssets(selectedAssets);
            this.contextMenuOpen = false;
        },
        folderRename:function(){
            console.log('Renaming the folder with ID "' + this.id + '"');
            // var filename = this.name.substr(0, this.name.lastIndexOf('.')) || this.name;
            // this.renameAsset(this.id, filename);
            this.contextMenuOpen = false;
        },
        folderCopy:function(){
            console.log('Opening copy modal window for asset with ID "' + this.id + '"');
            this.contextMenuOpen = false;
        },
        folderMove:function(){
            console.log('Opening move modal window for asset with ID "' + this.id + '"');
            this.contextMenuOpen = false;
        },
        folderSubscribe:function(){
            console.log('Subscribing to the asset with ID "' + this.id + '"');
            // var asset = {
            //     id: this.id,
            //     name: this.name,
            //     type: this.type,
            //     originalAssetId: this.originalAssetId
            // }
            // var selectedAssets= [asset];
            // this.subscribeToAssets(selectedAssets);
            this.contextMenuOpen = false;
        },
        folderUnsubscribe:function(){
            console.log('Unsubscribing from the folder with ID "' + this.id + '"');
            // var asset = {
            //     id: this.id,
            //     name: this.name,
            //     type: this.type,
            //     originalAssetId: this.originalAssetId
            // }
            // var selectedAssets= [asset];
            // this.unsubscribeFromAssets(selectedAssets);
            this.contextMenuOpen = false;
        },
        folderDelete:function(){
            console.log('Deleting asset with ID "' + this.id + '"');
            // var asset = {
            //     id: this.id,
            //     name: this.name,
            //     type: this.type,
            //     originalAssetId: this.originalAssetId
            // }
            // var selectedAssets= [asset];
            // this.deleteAssets(selectedAssets);
            this.contextMenuOpen = false;
        }
    }
}