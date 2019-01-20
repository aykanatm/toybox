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
          // TODO: Make dynamic
          userAvatarUrl: '../../images/users/test.png',
        }
    },
    watch:{
        isSelected:function(){
            this.$root.$emit('asset-selection-changed', this);
        }
    },
    computed:{
        thumbnailUrl:function(){
            return this.renditionUrl + '/renditions/' + this.id + '/t'
        },
        hasThumbnail:function(){
            return true;
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
        share:function(){
            console.log('Opening share modal window for asset with ID "' + this.assetId + '"');
        },
        download:function(){
            console.log('Downloading the file with ID "' + this.assetId + '"');
        },
        rename:function(){
            console.log('Renaming the file with ID "' + this.assetId + '"');
        },
        copy:function(){
            console.log('Opening copy modal window for asset with ID "' + this.assetId + '"');
        },
        move:function(){
            console.log('Opening move modal window for asset with ID "' + this.assetId + '"');
        },
        subscribe:function(){
            console.log('Subscribing to the asset with ID "' + this.assetId + '"');
        },
        delete:function(){
            console.log('Deleting asset with ID "' + this.assetId + '"');
        },
        showVersionHistory:function(){
            console.log('Showing version history of asset with ID "' + this.assetId + '"');
        }
    }
}