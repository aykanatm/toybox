module.exports = {
    props:{
        renditionUrl: String
    },
    data: function() {
        return  {
          componentName: 'Asset Preview Modal Window',
          asset: Object,
          hasPreview: true,
          canNavigateToNextAsset: false,
          canNavigateToPreviousAsset: false,
        }
    },
    mounted:function(){
        this.$root.$on('open-asset-preview-modal-window', (asset) => {
            console.log(asset);
            this.asset = asset;
            $(this.$el).modal('setting', 'closable', false).modal({
                autofocus: false,
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

            $('.combo.dropdown')
            .dropdown({
                action: 'combo'
            });

            this.$root.$emit('update-arrows-request', this.asset);
        });

        this.$root.$on('send-asset-to-preview', (asset) =>{
            this.asset = asset;
        });

        this.$root.$on('update-arrows', this.onUpdateArrows);
    },
    watch:{

    },
    computed:{

    },
    methods:{
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