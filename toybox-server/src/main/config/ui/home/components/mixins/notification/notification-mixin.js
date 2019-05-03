var notificationMixin = {
    data:function(){
        return{
            notifications:[]
        }
    },
    methods:{
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
    }
}