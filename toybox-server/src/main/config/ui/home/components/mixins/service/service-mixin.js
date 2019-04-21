var serviceMixin = {
    methods:{
        getService(serviceId){
            return axios.get("/services/" + serviceId)
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