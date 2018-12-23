module.exports = {
    props:{
        jobInstanceId: String,
        jobExecutionId: String,
        jobName: String,
        jobType: String,
        startTime: Number,
        endTime: Number,
        status: String,
        username: String
    },
    computed:{
        formattedStartTime(){
            if(this.startTime !== null){
                return this.convertToDateString(this.startTime);
            }
            return '';
        },
        formattedEndTime(){
            if(this.endTime !== null){
                return this.convertToDateString(this.endTime);
            }
            return '';
        }
    },
    data:function(){
        return{
            componentName: 'Job Details Modal Window',
            steps:[]
        }
    },
    mounted:function(){
        this.$root.$on('open-job-details-modal-window', (jobExecutionId) => {
            if(jobExecutionId === this.jobExecutionId)
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
                    var errorMessage = error.response.data.message
                    console.error(errorMessage);
                    this.$root.$emit('message-sent', 'Error', errorMessage);
                });
        },
        loadJobDetails:function(){
            this.getConfiguration("jobServiceUrl")
            .then(response => {
                if(response){
                    return axios.get(response.data.value + "/jobs/" + this.jobExecutionId + "/steps" )
                    .catch(error => {
                        var errorMessage = error.response.data.message
                        console.error(errorMessage);
                        this.$root.$emit('message-sent', 'Error', errorMessage);
                    });
                }
            })
            .then(response => {
                console.log(response);
                if(response){
                    this.steps = response.data.toyboxJobSteps;
                }
            });
        },
        convertToDateString(milliseconds){
            if(milliseconds && milliseconds !== ''){
                var date = new Date(milliseconds);
                var year = date.getFullYear();
                var month = date.getMonth() + 1 < 10 ? '0' + (date.getMonth() + 1) : (date.getMonth() + 1);
                var day = date.getDate() < 10 ? '0' + date.getDate() : date.getDate();
                var hours = date.getHours() < 10 ? '0' + date.getHours() : date.getHours();
                var minutes = date.getMinutes() < 10 ? '0' + date.getMinutes() : date.getMinutes();
                var seconds = date.getSeconds() < 10 ? '0' + date.getSeconds() : date.getSeconds();

                return year + '-' + month + '-' + day + ' ' + hours + ':' + minutes + ':' + seconds;
            }
            return '';
        }
    },
    components:{
        'job-step' : httpVueLoader('../job-step/job-step.vue')
    }
}