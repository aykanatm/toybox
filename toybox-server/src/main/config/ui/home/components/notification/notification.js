module.exports = {
    mixins:[serviceMixin],
    props:{
        id: Number,
        username: String,
        fromUsername: String,
        notification: String,
        notificationDate: Number,
        isRead: String,
        isDefaultNotification: Boolean,
        inNavbar: Boolean
    },
    data: function() {
        return  {
          componentName: 'Notification',
          avatarUrl: '',
          avatarLoadedSuccessfully: true,
          // Sorting
          sortType: 'DESC',
          sortColumn: 'date',
        }
    },
    mounted: function(){
        this.getService("toybox-rendition-loadbalancer")
            .then(response => {
                if(response){
                    var renditionUrl = response.data.value;
                    this.avatarUrl = renditionUrl + '/renditions/users/' + this.fromUsername;
                }
                else{
                    this.$root.$emit('message-sent', 'Error', "There was no response from the service endpoint!");
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
    computed:{
        friendlyDate:function(){
            return moment(this.notificationDate).format('MMMM Do YYYY, hh:mm:ss');
        }
    },
    watch:{
        avatarUrl:function(val){
            if(val !== ''){
                var self = this;
                $(self.$el).imagesLoaded()
                    .done( function() {
                        console.log('Notification avatar image loaded successfully!');
                        self.avatarLoadedSuccessfully = true;
                    })
                    .fail( function() {
                        console.log('Notification avatar image failed to load!');
                        self.avatarLoadedSuccessfully = false;
                    });
            }
        }
    },
    methods:{
        avatarFailedToLoad:function(){
            console.log('boo!');
            this.avatarLoadedSuccessfully = false;
        },
        markNotificationAsRead:function(event){
            console.log(event);
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
                                if(response){
                                    console.log(response);
                                    this.$root.$emit('message-sent', 'Success', response.data.message);

                                    var isNavbar = false;
                                    var paths = event.path;
                                    for(var i = 0; i < paths.length; i++){
                                        var path = paths[i];
                                        if(path.className === 'ui simple dropdown item toybox-navbar-item'){
                                            isNavbar = true;
                                            break;
                                        }
                                    }

                                    if(isNavbar){
                                        this.$root.$emit('notifications-updated', 0, 100, this.sortType, this.sortColumn, this.searchRequestFacetList, isNavbar);
                                    }
                                    else{
                                        this.$root.$emit('refresh-notifications');
                                    }
                                }
                                else{
                                    this.$root.$emit('message-sent', 'Error', "There was no response from the notification loadbalancer!");
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
                    else{
                        this.$root.$emit('message-sent', 'Error', "There was no response from the service endpoint!");
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