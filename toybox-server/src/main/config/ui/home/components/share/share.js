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
        canCopy: String,
        url: String
    },
    mounted:function(){
        $('.share-action-dropdown').dropdown();
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
            this.$root.$emit('open-share-modal-window', undefined, this.type, this.id);
        },
        deleteShare:function(){
            this.getService("toybox-share-loadbalancer")
                .then(response =>{
                    if(response){
                        var shareServiceUrl = response.data.value;

                        this.$root.$emit('start-delete-share');
                        axios.delete(shareServiceUrl + '/share/' + this.id, {headers:{}, data:{type: this.type}})
                            .then(response => {
                                if(response){
                                    this.$root.$emit('message-sent', 'Success', response.data.message);
                                    this.$root.$emit('end-delete-share');
                                    this.$root.$emit('refresh-shares');
                                }
                                else{
                                    this.$root.$emit('message-sent', 'Error', "There was no response from the share loadbalancer!");
                                }
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
                                this.$root.$emit('end-delete-share');
                                this.$root.$emit('message-sent', 'Error', errorMessage);
                            });
                    }
                    else{
                        this.$root.$emit('message-sent', 'Error', "There was no response from the service endpoint!");
                    }
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
        },
        copyUrl:function(){
            this.copyTextToClipboard(this.url, this);
        },
        copyTextToClipboard:function(text, share)
        {
            if (!navigator.clipboard)
            {
                this.fallbackCopyTextToClipboard(text, share);
                return;
            }

            navigator.clipboard.writeText(text)
                .then(function() {
                    share.$root.$emit('message-sent', 'Information', "Share URL copied to clipboard.");
            }, function(err) {
                share.$root.$emit('message-sent', 'Error', "An error occured while copying the URL to clipboard." + err);
            });
        },
        fallbackCopyTextToClipboard:function(text, share)
        {
            var textArea = document.createElement("textarea");
            textArea.value = text;
            document.body.appendChild(textArea);
            textArea.focus();
            textArea.select();

            try
            {
                var successful = document.execCommand('copy');
                if(successful){
                    share.$root.$emit('message-sent', 'Information', "Share URL copied to clipboard.");
                }
                else{
                    share.$root.$emit('message-sent', 'Error', "Could not copy the URL to clipboard.");
                }
            }
            catch (err)
            {
                share.$root.$emit('message-sent', 'Error', "An error occured while copying the URL to clipboard." + err);
            }

            document.body.removeChild(textArea);
        },
    }
}