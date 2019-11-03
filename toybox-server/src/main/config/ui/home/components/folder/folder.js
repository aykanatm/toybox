module.exports = {
    mixins:[itemActionsMixin, serviceMixin],
    props:{
        id: String,
        name: String,
        createdByUsername: String,
        subscribed: String,
        renditionUrl: String,
        shared: String,
        sharedByUsername: String,
        canDownload: String
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
            if(this.id === folderId){
                this.isSelected = false;
            }
        },
        folderShare:function(){
            var folder = {
                id: this.id,
                name: this.name,
                type: this.type,
                originalAssetId: this.originalAssetId,
                '@class': 'com.github.murataykanat.toybox.dbo.Container',
                parentContainerId: this.parentContainerId,
                shared: this.shared,
                sharedByUsername: this.sharedByUsername
            }
            var selectedFolders = [folder];
            this.shareItems(selectedFolders);
            this.contextMenuOpen = false;
        },
        folderDownload:function(){
            var folder = {
                id: this.id,
                name: this.name,
                type: this.type,
                originalAssetId: this.originalAssetId,
                '@class': 'com.github.murataykanat.toybox.dbo.Container',
                parentContainerId: this.parentContainerId,
                shared: this.shared,
                sharedByUsername: this.sharedByUsername
            }
            var selectedFolders = [folder];
            this.downloadItems(selectedFolders);
            this.contextMenuOpen = false;
        },
        folderRename:function(){
            this.renameItem(this.id, this.name);
            this.contextMenuOpen = false;
        },
        folderCopy:function(){
            var folder = {
                id: this.id,
                name: this.name,
                type: this.type,
                originalAssetId: this.originalAssetId,
                '@class': 'com.github.murataykanat.toybox.dbo.Container',
                parentContainerId: this.parentContainerId,
                shared: this.shared,
                sharedByUsername: this.sharedByUsername
            }
            var selectedFolders = [folder];
            this.copyItems(selectedFolders);
            this.contextMenuOpen = false;
        },
        folderMove:function(){
            var folder = {
                id: this.id,
                name: this.name,
                type: this.type,
                originalAssetId: this.originalAssetId,
                '@class': 'com.github.murataykanat.toybox.dbo.Container',
                parentContainerId: this.parentContainerId,
                shared: this.shared,
                sharedByUsername: this.sharedByUsername
            }
            var selectedFolders = [folder];
            this.moveItems(selectedFolders);
            this.contextMenuOpen = false;
        },
        folderSubscribe:function(){
            var folder = {
                id: this.id,
                name: this.name,
                type: this.type,
                originalAssetId: this.originalAssetId,
                '@class': 'com.github.murataykanat.toybox.dbo.Container',
                parentContainerId: this.parentContainerId,
                shared: this.shared,
                sharedByUsername: this.sharedByUsername
            }
            var selectedFolders = [folder];
            this.subscribeToItems(selectedFolders);
            this.contextMenuOpen = false;
        },
        folderUnsubscribe:function(){
            var folder = {
                id: this.id,
                name: this.name,
                type: this.type,
                originalAssetId: this.originalAssetId,
                '@class': 'com.github.murataykanat.toybox.dbo.Container',
                parentContainerId: this.parentContainerId,
                shared: this.shared,
                sharedByUsername: this.sharedByUsername
            }
            var selectedFolders = [folder];
            this.unsubscribeFromItems(selectedFolders);
            this.contextMenuOpen = false;
        },
        folderDelete:function(){
            var folder = {
                id: this.id,
                name: this.name,
                type: this.type,
                originalAssetId: this.originalAssetId,
                '@class': 'com.github.murataykanat.toybox.dbo.Container',
                parentContainerId: this.parentContainerId,
                shared: this.shared,
                sharedByUsername: this.sharedByUsername
            }
            var selectedFolders = [folder];
            this.deleteItems(selectedFolders);
            this.contextMenuOpen = false;
        }
    }
}