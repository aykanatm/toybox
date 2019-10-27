var userMixin = {
    data:function(){
        return{
            user:{
                username: '',
                name: '',
                lastname: '',
                avatarUrl: '',
                isAdmin: false
            },
        }
    },
    mounted:function(){
        this.getService("toybox-user-loadbalancer")
            .then(response => {
                if(response){
                    var userServiceUrl = response.data.value;
                    axios.get(userServiceUrl + "/users/me")
                        .then(userResponse => {
                            console.log(userResponse);
                            this.getService("toybox-rendition-loadbalancer")
                                .then(response => {
                                    if(response){
                                        var renditionUrl = response.data.value;

                                        this.user.username = userResponse.data.user.username;
                                        this.user.name = userResponse.data.user.name;
                                        this.user.lastname = userResponse.data.user.lastname;
                                        this.user.avatarUrl = renditionUrl + '/renditions/users/me';

                                        var roles = userResponse.data.user.roles;
                                        for(var i = 0; i < roles.length; i++){
                                            var role = roles[i];
                                            if(role.roleId == 2){
                                                this.user.isAdmin = true;
                                            }
                                        }
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