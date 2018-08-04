const folders = new Vue({
    el: '#toybox-folders',
    data:{
        view: 'folders',
    },
    components:{
        'navbar' : httpVueLoader('../components/navbar/navbar.vue')
    }
});