var configServiceMixin = {
    methods:{
        getConfiguration(fieldName){
            return axios.get("/configuration?field=" + fieldName)
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
    }
}