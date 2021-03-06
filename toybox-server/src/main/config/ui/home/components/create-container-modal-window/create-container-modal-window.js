module.exports = {
    data:function(){
        return{
            componentName: 'New Folder Modal Window',
            parentContainerId: '',
            containerName: '',
            containerUrl: ''
        }
    },
    mounted:function(){
        this.$root.$on('open-create-container-modal-window', (parentContainerId, containerUrl) => {
            this.parentContainerId = parentContainerId;
            this.containerUrl = containerUrl;
            this.containerName = '';

            $(this.$el).modal('setting', 'closable', false).modal('show');
        });
    },
    methods:{
        createContainer:function(){
            var createContainerRequest = {
                'parentContainerId': this.parentContainerId,
                'containerName': this.containerName
            }

            axios.post(this.containerUrl + '/containers', createContainerRequest)
                .then(response => {
                    if(response){
                        this.$root.$emit('message-sent', 'Success', response.data.message);
                        $(this.$el).modal('hide');
                    }
                    else{
                        this.$root.$emit('message-sent', 'Error', "There was no response from the folder loadbalancer!");
                    }
                })
                .catch(error => {
                    var errorMessage;
                    var isWarning = false;

                    if(error.response){
                        errorMessage = error.response.data.message
                        if(error.response.status == 401){
                            window.location = '/logout';
                        }
                        else if(error.response.status == 400){
                            isWarning = true;
                        }
                    }
                    else{
                        errorMessage = error.message;
                    }

                    console.error(errorMessage);
                    if(isWarning){
                        this.$root.$emit('message-sent', 'Warning', errorMessage);
                    }
                    else{
                        this.$root.$emit('message-sent', 'Error', errorMessage);
                    }

                    $(this.$el).modal('hide');
                });
        }
    }
}