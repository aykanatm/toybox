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
            this.$root.$emit('open-share-modal-window', undefined, this.id);
        },
        copyUrl:function(){
            this.copyTextToClipboard(this.url);
            this.$root.$emit('message-sent', 'Information', "Share URL copied to clipboard.");
        },
        copyTextToClipboard:function(text)
        {
            if (!navigator.clipboard)
            {
                this.fallbackCopyTextToClipboard(text);
                return;
            }

            navigator.clipboard.writeText(text)
                .then(function() {
                console.log('Async: Copying to clipboard was successful!');
            }, function(err) {
                console.error('Async: Could not copy text: ', err);
            });
        },
        fallbackCopyTextToClipboard:function(text)
        {
            var textArea = document.createElement("textarea");
            textArea.value = text;
            document.body.appendChild(textArea);
            textArea.focus();
            textArea.select();

            try
            {
                var successful = document.execCommand('copy');
                var msg = successful ? 'successful' : 'unsuccessful';
                console.log('Copying text command was ' + msg);
            }
            catch (err)
            {
                console.error('Fallback: Unable to copy', err);
            }

            document.body.removeChild(textArea);
        },
    }
}