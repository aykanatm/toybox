module.exports = {
    props:{
        stepExecutionId: String,
        stepName: String,
        startTime: Number,
        endTime: Number,
        status: String
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
    }
}