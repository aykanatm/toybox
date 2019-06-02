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
            // TODO: Implement open folder logic
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
            var folder = {
                id: this.id,
                name: this.name,
            }
            var selectedFolders = [folder];
            this.downloadItems(selectedFolders);
            this.contextMenuOpen = false;
        },
        folderRename:function(){
            console.log('Renaming the folder with ID "' + this.id + '"');
            this.renameItem(this.id, this.name);
            this.contextMenuOpen = false;
        },
        folderCopy:function(){
            console.log('Opening copy modal window for folder with ID "' + this.id + '"');
            this.contextMenuOpen = false;
        },
        folderMove:function(){
            console.log('Opening move modal window for folder with ID "' + this.id + '"');
            this.contextMenuOpen = false;
        },
        folderSubscribe:function(){
            console.log('Subscribing to the folder with ID "' + this.id + '"');
            var folder = {
                id: this.id,
                name: this.name,
            }
            var selectedFolders = [folder];
            this.subscribeToItems(selectedFolders);
            this.contextMenuOpen = false;
        },
        folderUnsubscribe:function(){
            console.log('Unsubscribing from the folder with ID "' + this.id + '"');
            var folder = {
                id: this.id,
                name: this.name,
            }
            var selectedFolders = [folder];
            this.unsubscribeFromItems(selectedFolders);
            this.contextMenuOpen = false;
        },
        folderDelete:function(){
            console.log('Deleting folder with ID "' + this.id + '"');
            var folder = {
                id: this.id,
                name: this.name,
            }
            var selectedFolders = [folder];
            this.deleteItems(selectedFolders);
            this.contextMenuOpen = false;
        }
    }
}