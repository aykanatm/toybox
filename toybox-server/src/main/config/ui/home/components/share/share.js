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
        $('.ui.dropdown').dropdown();
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