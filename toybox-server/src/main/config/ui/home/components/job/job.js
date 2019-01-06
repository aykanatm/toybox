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
        }
    }
}