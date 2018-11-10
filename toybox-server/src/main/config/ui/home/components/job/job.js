module.exports = {
    props:{
        jobInstanceId: Number,
        jobName: String,
        jobType: String,
        startTime: Number,
        endTime: Number,
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
        convertToDateString(milliseconds){
            var date = new Date(milliseconds);
            var year = date.getFullYear();
            var month = date.getMonth() + 1
            var day = date.getDate();
            var hours = date.getHours();
            var minutes = date.getMinutes();
            var seconds = date.getSeconds();

            return year + '-' + month + '-' + day + ' ' + hours + ':' + minutes + ':' + seconds;
        }
    }
}