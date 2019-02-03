module.exports = {
    props:{
        renditionUrl: String
    },
    data: function() {
        return  {
          componentName: 'Asset Preview Modal Window',
          asset: Object,
          hasPreview: true,
          isFirstRendered: true
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
        });
    },
    watch:{

    },
    computed:{
        previewUrl:function(){
            return this.renditionUrl + '/renditions/assets/' + this.asset.id + '/p'
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
        }
    },
    methods:{
        onPreviewImgSrcNotFound:function(){
            if(!isFirstRendered)
            {
                this.hasPreview = false;
            }
            // Makes sure that when the modal window is first rendered as empty, it will not set the hasPreview to false
            this.isFirstRendered = false;
        }
    }
}