module.exports = {
    mixins:[serviceMixin],
    props:{
        id: Number,
        username: String,
        fromUsername: String,
        notification: String,
        notificationDate: Number,
        isRead: String,
        isDefaultNotification: Boolean
    },
    data: function() {
        return  {
          componentName: 'Notification',
          avatarUrl: '',
        }
    },
    mounted: function(){
        this.getService("toybox-rendition-loadbalancer")
            .then(response => {
                var renditionUrl = response.data.value;
                this.avatarUrl = renditionUrl + '/renditions/users/' + this.fromUsername;
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
    computed:{
        friendlyDate:function(){
            return moment(this.notificationDate).format('MMMM Do YYYY, hh:mm:ss');
        }
    },
    methods:{
        markNotificationAsRead:function(){
            this.getService("toybox-notification-loadbalancer")
                .then(response => {
                    if(response){
                        console.log(response);
                        var notificationUpdateRequest = {
                            'notificationIds':[this.id],
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
        }
    }
}