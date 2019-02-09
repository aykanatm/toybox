module.exports = {
    props:{
        renditionUrl: String
    },
    data: function() {
        return  {
          componentName: 'Asset Preview Modal Window',
          asset: Object,
          hasPreview: true,
          isFirstRendered: true,
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

        this.$root.$on('display-asset-in-preview', (asset) =>{
            this.asset = asset;
        });

        this.$root.$on('update-arrows', this.onUpdateArrows);
    },
    watch:{

    },
    computed:{
        previewUrl:function(){
            if(this.asset.id !== undefined){
                return this.renditionUrl + '/renditions/assets/' + this.asset.id + '/p'
            }
            else{
                return null;
            }
        },
        isImage:function(){
            return this.asset.isImage;
        },
        isVideo:function(){
            return this.asset.isVideo;
        },
        isAudio:function(){
            return this.asset.isAudio;
        },
        isPdf:function(){
            return this.asset.isPdf;
        },
        isWord:function(){
            return this.asset.isWord;
        },
        isExcel:function(){
            return this.asset.isExcel;
        },
        isPowerpoint:function(){
            return this.asset.isPowerpoint;
        },
        isArchive:function(){
            return this.asset.isArchive;
        },
        assetName:function(){
            return this.asset.name;
        }
    },
    methods:{
        onPreviewImgSrcNotFound:function(){
            if(!this.isFirstRendered)
            {
                this.hasPreview = false;
            }
            // Makes sure that when the modal window is first rendered as empty, it will not set the hasPreview to false
            this.isFirstRendered = false;
        },
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
        }
    }
}