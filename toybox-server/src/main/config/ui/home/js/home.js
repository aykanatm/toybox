const home = new Vue({
    el: '#toybox-home',
    mixins:[messageMixin, userMixin, configServiceMixin],
    data:{
        view: 'home',
    },
    mounted:function(){
        var csrfHeader = $("meta[name='_csrf_header']").attr("content");
        var csrfToken = $("meta[name='_csrf']").attr("content");
        console.log('CSRF header: ' + csrfHeader);
        console.log('CSRF token: ' + csrfToken);
        axios.defaults.headers = {
            // 'X-CSRF-TOKEN': csrfToken,
            'XSRF-TOKEN': csrfToken
        }
        axios.defaults.withCredentials = true;

        this.$root.$on('message-sent', this.displayMessage);
    },
    methods:{

    },
    components:{
        'navbar' : httpVueLoader('../components/navbar/navbar.vue'),
        'message' : httpVueLoader('../components/message/message.vue')
    }
});