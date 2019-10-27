module.exports = {
    mixins:[serviceMixin],
    props:{
        jobInstanceId: String,
        jobExecutionId: String,
        jobName: String,
        jobType: String,
        startTime: Number,
        endTime: Number,
        status: String,
        username: String,
        steps: Array
    },
    computed:{
        isCompleted(){
            return this.status === 'COMPLETED' || this.status === 'STOPPED' || this.status === 'STOPPING';
        },
        isImport(){
            return this.jobType === 'IMPORT';
        },
        isExport(){
            return this.jobType === 'EXPORT';
        },
        formattedStartTime(){
            if(this.startTime !== null){
                return convertToDateString(this.startTime);
            }
            return '';
        },
        formattedEndTime(){
            if(this.endTime !== null){
                return convertToDateString(this.endTime);
            }
            return '';
        }
    },
    methods:{
        showJobDetailsModalWindow:function(){
            this.$root.$emit('open-job-details-modal-window', this.jobInstanceId);
        },
        downloadJobResult:function(){
            this.getService("toybox-job-loadbalancer")
            .then(response =>{
                if(response){
                    return axios.get(response.data.value + '/jobs/download/' + this.jobInstanceId, {responseType:'blob'})
                        .then(response =>{
                            if(response){
                                console.log(response);
                                var filename = 'Download.zip';
                                var blob = new Blob([response.data], {type:'application/octet-stream'});
                                saveAs(blob , filename);
                            }
                            else{
                                this.$root.$emit('message-sent', 'Error', "There was no response from the job loadbalancer!");
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
            });
        },
        stopJob:function(){
            this.getService("toybox-job-loadbalancer")
            .then(response =>{
                if(response){
                    return axios.post(response.data.value + '/jobs/stop/' + this.jobInstanceId)
                        .then(response =>{
                            if(response){
                                console.log(response);
                                this.$root.$emit('message-sent', 'Success', response.data.message);
                            }
                            else{
                                this.$root.$emit('message-sent', 'Error', "There was no response from the job loadbalancer!");
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
            });
        }
    }
}