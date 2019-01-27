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
        .then(response => {
            console.log(response);
            this.user.username = response.data.user.username;
            this.user.name = response.data.user.name;
            this.user.lastname = response.data.user.lastname;
            this.user.avatarUrl = 'http://localhost:8103/renditions/users/' + response.data.user.id;
        })
    }
}