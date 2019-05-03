module.exports = {
    mixins:[serviceMixin],
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
            notifications:[]
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
        getNotifications:function(){
            this.getService("toybox-notification-loadbalancer")
            .then(response => {
                if(response){
                    var searchRequest = {
                        'fromUsername': null,
                        'content': '*',
                        'notificationDate': new Date(),
                        'isRead': 'N'
                    }

                    return axios.post(response.data.value + "/notifications/search", searchRequest)
                        .then(response => {
                            this.notifications = response.data.notifications;
                        })
                        .catch(error => {
                            var errorMessage;

                            if(error.response){
                                errorMessage = error.response.data.message
                                if(error.response.status == 401){
                                    window.location = '/logout';
                                }
                            }
                            else{
                                errorMessage = error.message;
                            }

                            console.error(errorMessage);
                            this.$root.$emit('message-sent', 'Error', errorMessage);
                        });
                }
            })
            .catch(error => {
                var errorMessage;

                if(error.response.status == 401){
                    window.location = '/logout';
                }
                else{
                    if(error.response){
                        errorMessage = error.response.data.message
                    }
                    else{
                        errorMessage = error.message;
                    }

                    console.error(errorMessage);
                    this.$root.$emit('message-sent', 'Error', errorMessage);
                }
            });
        },
        markAllNotificationAsRead:function(){
            this.getService("toybox-notification-loadbalancer")
                .then(response => {
                    if(response){
                        console.log(response);
                        var notificationIds =[];
                        for(var i = 0; i < this.notifications.length; i++){
                            var notification = this.notifications[i];
                            notificationIds.push(notification.id);
                        }
                        var notificationUpdateRequest = {
                            'notificationIds': notificationIds,
                            'isRead': 'Y'
                        }

                        return axios.patch(response.data.value + '/notifications', notificationUpdateRequest)
                            .then(response => {
                                console.log(response);
                                this.$root.$emit('message-sent', 'Success', response.data.message);
                                this.$root.$emit('notifications-updated');
                            })
                            .catch(error => {
                                var errorMessage;

                                if(error.response){
                                    errorMessage = error.response.data.message
                                    if(error.response.status == 401){
                                        window.location = '/logout';
                                    }
                                }
                                else{
                                    errorMessage = error.message;
                                }

                                console.error(errorMessage);
                                this.$root.$emit('message-sent', 'Error', errorMessage);
                            });
                    }
                })
                .catch(error => {
                    var errorMessage;

                    if(error.response){
                        errorMessage = error.response.data.message
                        if(error.response.status == 401){
                            window.location = '/logout';
                        }
                    }
                    else{
                        errorMessage = error.message;
                    }

                    console.error(errorMessage);
                    this.$root.$emit('message-sent', 'Error', errorMessage);
                });
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