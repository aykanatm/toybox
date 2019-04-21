var userMixin = {
    data:function(){
        return{
            user:{
                username: '',
                name: '',
                lastname: '',
                avatarUrl: ''
            },
        }
    },
    mounted:function(){
        axios.get("/me")
            .then(userResponse => {
                console.log(userResponse);
                this.getService("toybox-rendition-loadbalancer")
                .then(response => {
                    var renditionUrl = response.data.value;

                    this.user.username = userResponse.data.user.username;
                    this.user.name = userResponse.data.user.name;
                    this.user.lastname = userResponse.data.user.lastname;
                    this.user.avatarUrl = renditionUrl + '/renditions/users/me';
                });
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
}