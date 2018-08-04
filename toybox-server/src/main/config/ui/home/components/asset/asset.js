module.exports = {
    props:{
        assetId: String,
        assetName: String,
        importedBy: String,
        userAvatarUrl: String,
        thumbnailUrl: String,
        extension: String,
    },
    data: function() {
        return  {
          componentName: 'Asset',
          isSelected: false
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
        }
    }
}