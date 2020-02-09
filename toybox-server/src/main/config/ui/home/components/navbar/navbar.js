module.exports = {
    mixins:[serviceMixin, notificationMixin, facetMixin],
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
            userInitialized: false,
            searchQuery:''
        }
    },
    mounted:function(){
        this.getNotifications(null, '*', new Date(), 'N', 0, 100, this.searchRequestFacetList, true);
        this.$root.$on('notifications-updated', this.getNotifications);
        this.$root.$on('clear-search-query', this.clearSearchQuery);
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
            this.$root.$emit('open-import-modal-window', '');
        },
        navigateToNotificationsPage:function(){
            window.location = "/toybox/notifications";
        },
        logout:function(){
            window.location = "/logout";
        },
        onSubmit:function(){
            this.$root.$emit('perform-contextual-search', this.searchQuery);
        },
        clearSearchQuery:function(){
            this.searchQuery = '';
        }
    },
    components:{
        'notification' : httpVueLoader('../notification/notification.vue'),
        'import-modal-window' : httpVueLoader('../import-modal-window/import-modal-window.vue'),
    }
}