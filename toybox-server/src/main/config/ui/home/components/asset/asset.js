module.exports = {
    props:{
        id: String,
        name: String,
        importedByUsername: String,
        extension: String,
    },
    data: function() {
        return  {
          componentName: 'Asset',
          isSelected: false,
          userAvatarUrl: '',
        }
    },
    watch:{
        isSelected:function(){
            this.$root.$emit('asset-selection-changed', this);
        }
    },
    computed:{
        thumbnailUrl:function(){
            return ''
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