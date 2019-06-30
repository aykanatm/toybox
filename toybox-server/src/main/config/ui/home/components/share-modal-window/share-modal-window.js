module.exports = {
    data:function(){
        return{
            componentName: 'Share Modal Window',
            selectedAssets: '',
            users:[],
            // User type
            isExternalUser: false,
            // Notification
            notifyMe: false,
            notifyOnView: false,
            notifyOnEdit: false,
            notifyOnDownload: false,
            notifyOnShare: false,
            notifyOnMoveOrCopy: false,
            // Share permissions
            canView: true,
            canEdit: false,
            canDownload: false,
            canShare: false,
            canMoveOrCopy: false,
            externalShareUrl:''
        }
    },
    mounted:function(){
        this.$root.$on('open-share-modal-window', (selectedAssets) => {
            this.selectedAssets = selectedAssets;

            // TODO: Fetch users and populate the users list

            this.isExternalUser = false;

            this.notifyMe = false;
            this.notifyOnView = false;
            this.notifyOnEdit = false;
            this.notifyOnDownload = false;
            this.notifyOnShare = false;
            this.notifyOnMoveOrCopy = false;

            this.canView = true;
            this.canEdit = false;
            this.canDownload = false;
            this.canShare = false;
            this.canMoveOrCopy = false;

            this.externalShareUrl = ''

            $('#user-dropdown').dropdown();

            $(this.$el).modal('setting', 'closable', false).modal('show');
        });
    },
    watch:{
        notifyMe:function(value){
            if(!value){
                this.notifyOnView = false;
                this.notifyOnEdit = false;
                this.notifyOnDownload = false;
                this.notifyOnShare = false;
                this.notifyOnMoveOrCopy = false;
            }
        },
        isExternalUser:function(value){
            if(value){
                this.notifyOnView = false;
                this.notifyOnEdit = false;
                this.notifyOnShare = false;
                this.notifyOnMoveOrCopy = false;

                this.canView = false;
                this.canEdit = false;
                this.canDownload = true;
                this.canShare = false;
                this.canMoveOrCopy = false;
            }
        }
    },
    methods:{
        generateUrl:function(){
            // TODO: Generate external share url
        },
        share:function(){
            // TODO: Generate internal share and close the window
        },
        copy:function(){
            this.copyTextToClipboard(this.externalShareUrl);
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
        }
    }
}