module.exports = {
    props:{
        id: String,
        name: String,
        importedByUsername: String,
        extension: String,
        renditionUrl: String,
        type: String
    },
    data: function() {
        return  {
          componentName: 'Asset',
          isSelected: false,
          hasThumbnail: true,
          contextMenuOpen: false
        }
    },
    watch:{
        isSelected:function(){
            this.$root.$emit('asset-selection-changed', this);
        }
    },
    computed:{
        userAvatarUrl:function(){
            return this.renditionUrl + '/renditions/users/' + this.importedByUsername;
        },
        thumbnailUrl:function(){
            return this.renditionUrl + '/renditions/assets/' + this.id + '/t'
        },
        isImage:function(){
            return this.type.startsWith('image') || this.type === 'application/postscript';
        },
        isVideo:function(){
            return this.type.startsWith('video');
        },
        isAudio:function(){
            return this.type.startsWith('audio');
        },
        isPdf:function(){
            return this.type === 'application/pdf';
        },
        isWord:function(){
            return this.type === 'application/msword' || this.type === 'application/vnd.openxmlformats-officedocument.wordprocessingml.document';
        },
        isExcel:function(){
            return this.type === 'application/vnd.ms-excel' || this.type === 'application/vnd.openxmlformats-officedocument.spreadsheetml.sheet';
        },
        isPowerpoint:function(){
            return this.type === 'application/vnd.ms-powerpoint' || this.type === 'application/vnd.openxmlformats-officedocument.presentationml.presentation';
        },
        isArchive:function(){
            return this.type === 'application/zip';
        }
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
        onThumbnailImgSrcNotFound:function(){
            this.hasThumbnail = false;
        },
        share:function(){
            console.log('Opening share modal window for asset with ID "' + this.id + '"');
            this.contextMenuOpen = false;
        },
        download:function(){
            console.log('Downloading the file with ID "' + this.id + '"');
            this.contextMenuOpen = false;
        },
        rename:function(){
            console.log('Renaming the file with ID "' + this.id + '"');
            this.contextMenuOpen = false;
        },
        copy:function(){
            console.log('Opening copy modal window for asset with ID "' + this.id + '"');
            this.contextMenuOpen = false;
        },
        move:function(){
            console.log('Opening move modal window for asset with ID "' + this.id + '"');
            this.contextMenuOpen = false;
        },
        subscribe:function(){
            console.log('Subscribing to the asset with ID "' + this.id + '"');
            this.contextMenuOpen = false;
        },
        delete:function(){
            console.log('Deleting asset with ID "' + this.id + '"');
            this.contextMenuOpen = false;
        },
        showVersionHistory:function(){
            console.log('Showing version history of asset with ID "' + this.id + '"');
            this.contextMenuOpen = false;
        }
    }
}