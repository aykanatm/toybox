module.exports = {
    props:{
        id: String,
        name: String,
        version: Number,
        date: Number,
        type: String,
        size: String,
        username: String,
        renditionUrl: String,
        isLatestVersion: String,
        assetUrl: String
    },
    data:function(){
        return{
            hasThumbnail: true,
            isHovered: false,
            isImage: false,
            isVideo: false,
            isAudio: false,
            isPdf: false,
            isWord: false,
            isExcel: false,
            isPowerpoint: false,
            isDocument: false,
            isArchive: false
        }
    },
    mounted:function(){
        this.isImage = this.type.startsWith('image') || this.type === 'application/postscript';
        this.isVideo = this.type.startsWith('video');
        this.isAudio = this.type.startsWith('audio');
        this.isPdf = this.type === 'application/pdf';
        this.isWord = this.type === 'application/msword' || this.type === 'application/vnd.openxmlformats-officedocument.wordprocessingml.document';
        this.isExcel = this.type === 'application/vnd.ms-excel' || this.type === 'application/vnd.openxmlformats-officedocument.spreadsheetml.sheet';
        this.isPowerpoint = this.type === 'application/vnd.ms-powerpoint' || this.type === 'application/vnd.openxmlformats-officedocument.presentationml.presentation';
        this.isDocument = this.isPdf || this.isWord || this.isExcel || this.isPowerpoint;
        this.isArchive = this.type === 'application/zip';
    },
    computed:{
        importDate:function(){
            return convertToDateString(this.date);
        },
        thumbnailUrl:function(){
            return this.renditionUrl + '/renditions/assets/' + this.id + '/t';
        }
    },
    methods:{
        onThumbnailImgSrcNotFound:function(){
            this.hasThumbnail = false;
        },
        onMouseEnter:function(){
            this.isHovered = true;
        },
        onMouseLeave:function(){
            this.isHovered = false;
        },
        revert:function(){
            console.log('Reverting to version ' + this.version + '...');
            var revertToVersionRequest = {
                'version': this.version
            }
            axios.post(this.assetUrl + '/assets/' + this.id + '/revert', revertToVersionRequest)
                .then(response => {
                    console.log(response.data);
                    this.$root.$emit('message-sent', 'Success', response.data.message);
                    this.$root.$emit('refresh-asset-version-history');
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
    }
}