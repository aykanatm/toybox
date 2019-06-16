var notificationMixin = {
    data:function(){
        return{
            notifications:[]
        }
    },
    methods:{
        getNotifications:function(fromUsername, content, notificationDate, isRead, offset, limit, searchRequestFacetList, fromNavbar){
            this.getService("toybox-notification-loadbalancer")
            .then(response => {
                if(response){
                    var searchRequest = {
                        'fromUsername': fromUsername,
                        'content': content,
                        'notificationDate': notificationDate,
                        'isRead': isRead,
                        'searchRequestFacetList': searchRequestFacetList,
                        'limit' : limit,
                        'offset' : offset
                    }

                    return axios.post(response.data.value + "/notifications/search", searchRequest)
                        .then(response => {
                            if(response.status == 204){
                                this.notifications = [];
                                if(!fromNavbar){
                                    this.facets = [];
                                    this.totalRecords = 0;
                                    this.totalPages = 0;
                                    this.currentPage = 0;
                                    this.$root.$emit('message-sent', 'Information', 'You do not have any notifications.');
                                }
                            }
                            else{
                                this.notifications = response.data.notifications;
                                if(!fromNavbar){
                                    this.facets = response.data.facets;
                                    this.totalRecords = response.data.totalRecords;
                                    this.totalPages = Math.ceil(this.totalRecords / this.limit);
                                    this.currentPage = Math.ceil((this.offset / this.limit) + 1);
                                }
                            }

                            if(this.totalRecords == 0){
                                this.startIndex = 0;
                                this.endIndex = 0
                            }
                            else{
                                this.startIndex = this.offset + 1;
                                this.endIndex = (this.offset + this.limit) < this.totalRecords ? (this.offset + this.limit) : this.totalRecords;
                            }

                            if(this.currentPage == 1){
                                if(this.totalPages != 1){
                                    this.nextPageButtonDisabled = false;
                                    this.previousPageButtonDisabled = true;
                                }
                                else{
                                    this.nextPageButtonDisabled = true;
                                    this.previousPageButtonDisabled = true;
                                }
                            }
                            else if(this.currentPage == this.totalPages){
                                this.nextPageButtonDisabled = true;
                                this.previousPageButtonDisabled = false;
                            }
                            else{
                                this.nextPageButtonDisabled = false;
                                this.previousPageButtonDisabled = false;
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
        markAllNotificationAsRead:function(event){
            console.log(event);
            this.getService("toybox-notification-loadbalancer")
                .then(response => {
                    if(response){
                        console.log(response);
                        var notificationIds = [0];
                        var notificationUpdateRequest = {
                            'notificationIds': notificationIds,
                            'isRead': 'Y'
                        }

                        return axios.patch(response.data.value + '/notifications', notificationUpdateRequest)
                            .then(response => {
                                console.log(response);
                                this.$root.$emit('message-sent', 'Success', response.data.message);

                                if($(event.target.parentElement).attr('class') === 'navbar-mark-all-notifications-as-read-button'){
                                    this.$root.$emit('notifications-updated', null, '*', new Date(), 'N', 0, 100, this.searchRequestFacetList, true);
                                }
                                else{
                                    this.$root.$emit('refresh-notifications');
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