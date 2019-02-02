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
        .catch(error => {
                var errorMessage;

                if(error.response){
                    errorMessage = error.response.data.message
                }
                else{
                    errorMessage = error.message;
                }

                console.error(errorMessage);
                this.$root.$emit('message-sent', 'Error', errorMessage);
        })
        .then(userResponse => {
            console.log(userResponse);
            this.getConfiguration("renditionServiceUrl")
            .then(response => {
                var renditionUrl = response.data.value;

                this.user.username = userResponse.data.user.username;
                this.user.name = userResponse.data.user.name;
                this.user.lastname = userResponse.data.user.lastname;
                this.user.avatarUrl = renditionUrl + '/renditions/users/' + userResponse.data.user.username;
            });
        })
    }
}