const home = new Vue({
    el: '#toybox-home',
    data:{
        view: 'home',
    },
    components:{
        'navbar' : httpVueLoader('../components/navbar/navbar.vue')
    }
});