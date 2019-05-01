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
                notificationDate: new Date(),
                isRead: false
            },
            userInitialized: false,
            notifications:[]
        }
    },
    mounted:function(){
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
    watch:{
        user:{
            handler(user){
                if(user.avatarUrl !== ''){
                    this.userInitialized = true;
                    var self = this;
                    setTimeout(() => {
                        $('#toybox-profile-menu').imagesLoaded()
                        .done( function() {
                            console.log('all images successfully loaded');
                            self.userInitialized = true;
                        })
                        .fail( function() {
                            console.log('all images loaded, at least one is broken');
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