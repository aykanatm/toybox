module.exports = {
    mixins:[serviceMixin, notificationMixin],
    props:{
        user: Object
    },
    data: function() {
        return  {
            componentName: 'Navigation Bar',
            defaultNotification:{
                id:0,
                fromUsername: 'system',
                notification: 'There are no new unread notifications',
                notificationDate: new Date().getTime(),
                isRead: 'Y'
            },
            userInitialized: false
        }
    },
    mounted:function(){
        this.getNotifications();
        this.$root.$on('notifications-updated', this.getNotifications);
    },
    watch:{
        user:{
            handler(user){
                if(user.avatarUrl !== ''){
                    this.userInitialized = true;
                    var self = this;
                    setTimeout(() => {
                        $('#toybox-profile-menu').imagesLoaded()
                        .done( function() {
                            console.log('User image loaded successfully!');
                            self.userInitialized = true;
                        })
                        .fail( function() {
                            console.log('User image failed to load!');
                            self.userInitialized = false;
                        });
                    }, 200);
                }
            },
            deep: true
        }
    },
    methods:{
        showUploadModalWindow:function(){
            $('#toybox-import-modal-window').modal('show');
        },
        logout:function(){
            window.location = "/logout";
        }
    },
    components:{
        'notification' : httpVueLoader('../notification/notification.vue'),
        'import-modal-window' : httpVueLoader('../import-modal-window/import-modal-window.vue'),
    }
}