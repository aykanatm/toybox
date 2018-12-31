module.exports = {
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
    methods:{
        showJobDetailsModalWindow:function(){
            this.$root.$emit('open-job-details-modal-window', this.jobInstanceId);
        },
        convertToDateString(milliseconds){
            if(milliseconds){
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
        'job-details-modal-window' : httpVueLoader('../job-details-modal-window/job-details-modal-window.vue')
    }
}