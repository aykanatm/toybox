module.exports = {
    props:{
        jobInstanceId: Number,
        jobName: String,
        jobType: String,
        startTime: String,
        endTime: String,
        status: String
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
        }
    }
}