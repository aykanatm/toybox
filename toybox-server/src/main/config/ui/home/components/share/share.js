module.exports = {
    mixins:[serviceMixin],
    props:{
        id: String,
        type: String,
        username: String,
        creationDate: Number,
        enableExpire: String,
        expirationDate: Number,
        enableUsageLimit: String,
        maxNumberOfHits: Number,
        notifyOnEdit: String,
        notifyOnDownload: String,
        notifyOnShare: String,
        notifyOnCopy: String,
        canEdit: String,
        canDownload: String,
        canShare: String,
        canCopy: String
    },
    computed:{
        formattedCreationDate(){
            if(this.creationDate !== null){
                return convertToDateString(this.creationDate);
            }
            return '';
        },
        formattedExpirationDate(){
            if(this.expirationDate !== null){
                return convertToDateString(this.expirationDate);
            }
            return '';
        }
    },
    methods:{
        editShare:function(){
            this.$root.$emit('edit-share', this.internalShareId);
        }
    }
}