module.exports = {
    props:{
        jobInstanceId: String
    },
    computed:{
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
    data:function(){
        return{
            componentName: 'Job Details Modal Window',
            jobName: null,
            jobType: null,
            startTime: null,
            endTime: null,
            status: null,
            username: null,
            steps: null,
        }
    },
    mounted:function(){
        this.$root.$on('open-job-details-modal-window', (jobInstanceId) => {
            if(jobInstanceId === this.jobInstanceId)
            {
                $(this.$el).modal('show');
                this.loadJobDetails();
            }
        });
    },
    methods:{
        getConfiguration(fieldName){
            return axios.get("/configuration?field=" + fieldName)
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
        },
        loadJobDetails:function(){
            this.getConfiguration("jobServiceUrl")
            .then(response => {
                if(response){
                    return axios.get(response.data.value + "/jobs/" + this.jobInstanceId)
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
            })
            .then(response => {
                console.log(response);
                if(response){
                    this.jobName = response.data.toyboxJob.jobName;
                    this.jobType = response.data.toyboxJob.jobType;
                    this.startTime = response.data.toyboxJob.startTime;
                    this.endTime = response.data.toyboxJob.endTime;
                    this.status = response.data.toyboxJob.status;
                    this.username = response.data.toyboxJob.username;
                    this.steps = response.data.toyboxJob.steps;
                }
            });
        }
    },
    components:{
        'job-step' : httpVueLoader('../job-step/job-step.vue')
    }
}