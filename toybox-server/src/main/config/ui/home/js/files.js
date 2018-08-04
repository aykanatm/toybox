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
        ]
    },
    components:{
        'navbar' : httpVueLoader('../components/navbar/navbar.vue'),
        'asset' : httpVueLoader('../components/asset/asset.vue')
    }
});