module.exports = {
    mixins:[configServiceMixin],
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
            return this.status === 'COMPLETED';
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
            this.getConfiguration("jobServiceUrl")
            .then(response =>{
                if(response){
                    return axios.get(response.data.value + '/jobs/download/' + this.jobInstanceId, {responseType:'blob'})
                        .then(response =>{
                            console.log(response);
                            var filename = 'Download.zip';
                            var mimeType = 'application/zip';

                            if (typeof window.chrome !== 'undefined') {
                                // Chrome version
                                var link = document.createElement('a');
                                link.href = window.URL.createObjectURL(response.data);
                                link.download = filename;
                                document.body.appendChild(link)
                                link.click();
                            } else if (typeof window.navigator.msSaveBlob !== 'undefined') {
                                // IE version
                                var blob = new Blob([response.data], { type: mimeType });
                                window.navigator.msSaveBlob(blob, filename);
                            } else {
                                // Firefox version
                                var file = new File([response.data], filename, { type: 'application/force-download' });
                                window.open(URL.createObjectURL(file));
                            }
                        })
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
                        });
                }
            });
        }
    }
}