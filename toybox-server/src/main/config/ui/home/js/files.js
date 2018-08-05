const files = new Vue({
    el: '#toybox-files',
    data:{
        view: 'files',
        // Dummy assets
        assets:[
            {
                assetId: '123456',
                assetName: 'filename_1.ext',
                userAvatarUrl: 'http://via.placeholder.com/250x250',
                importedBy: 'Username',
                thumbnailUrl: 'http://via.placeholder.com/300x250',
                extension: 'EXT',
                isSelected: false
            },
            {
                assetId: '45677',
                assetName: 'filename_2.ext',
                userAvatarUrl: 'http://via.placeholder.com/250x250',
                importedBy: 'Username',
                thumbnailUrl: 'http://via.placeholder.com/200x200',
                extension: 'EXT',
                isSelected: false
            },
            {
                assetId: '45677212423',
                assetName: 'filename_2.ext',
                userAvatarUrl: 'http://via.placeholder.com/250x250',
                importedBy: 'Username',
                thumbnailUrl: 'http://via.placeholder.com/300x100',
                extension: 'EXT',
                isSelected: false
            }
        ],
        selectedAssets:[]
    },
    methods:{
        onAssetSelectionChanged:function(asset){
            if(asset.isSelected){
                this.selectedAssets.push(asset);
            }
            else{
                this.selectedAssets.splice(asset, 1);
            }

            console.log(this.selectedAssets);
        },
        share:function(){
            console.log('Sharing the following assets:');
            for(var i = 0; i < this.selectedAssets.length; i++)
            {
                var asset = this.selectedAssets[i];
                console.log(asset.assetName + '/ ' +  asset.assetId);
            }
        },
        download:function(){
            console.log('Downloading the following assets:');
            for(var i = 0; i < this.selectedAssets.length; i++)
            {
                var asset = this.selectedAssets[i];
                console.log(asset.assetName + '/ ' +  asset.assetId);
            }
        },
        copy:function(){
            console.log('Copying the following assets:');
            for(var i = 0; i < this.selectedAssets.length; i++)
            {
                var asset = this.selectedAssets[i];
                console.log(asset.assetName + '/ ' +  asset.assetId);
            }
        },
        move:function(){
            console.log('Moving the following assets:');
            for(var i = 0; i < this.selectedAssets.length; i++)
            {
                var asset = this.selectedAssets[i];
                console.log(asset.assetName + '/ ' +  asset.assetId);
            }
        },
        subscribe:function(){
            console.log('Subscribing to the following assets:');
            for(var i = 0; i < this.selectedAssets.length; i++)
            {
                var asset = this.selectedAssets[i];
                console.log(asset.assetName + '/ ' +  asset.assetId);
            }
        },
        delete:function(){
            console.log('Deleting the following assets:');
            for(var i = 0; i < this.selectedAssets.length; i++)
            {
                var asset = this.selectedAssets[i];
                console.log(asset.assetName + '/ ' +  asset.assetId);
            }
        },
    },
    components:{
        'navbar' : httpVueLoader('../components/navbar/navbar.vue'),
        'asset' : httpVueLoader('../components/asset/asset.vue')
    }
});