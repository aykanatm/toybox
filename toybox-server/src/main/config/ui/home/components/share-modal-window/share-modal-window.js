module.exports = {
    mixins:[serviceMixin, userMixin],
    data:function(){
        return{
            componentName: 'Share Modal Window',
            selectionContext: '',
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
            // Models
            externalShareUrl: '',
            expirationDate: '',
            maxNumberOfHits: -1
        }
    },
    mounted:function(){
        this.$root.$on('open-share-modal-window', (selectionContext) => {
            this.getService("toybox-user-loadbalancer")
                .then(response => {
                    var userServiceUrl = response.data.value;
                    axios.get(userServiceUrl + "/users")
                        .then(response => {
                            console.log(response);
                            var users = response.data.users;

                            for(var i = 0; i < users.length; i++){
                                var user = users[i];

                                if(this.user.username !== user.username){
                                    this.users.push({
                                        'displayName' : user.name + ' ' + user.lastname + ' (' + user.username + ')',
                                        'userId': user.id
                                    });
                                }
                            }

                            this.selectionContext = selectionContext;

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
            this.getService("toybox-share-loadbalancer")
                .then(response =>{
                    var shareServiceUrl = response.data.value;
                    var externalShareRequest = {
                        selectionContext: this.selectionContext,
                        expirationDate: this.expirationDate,
                        maxNumberOfHits: this.maxNumberOfHits,
                        notifyWhenDownloaded: this.notifyOnDownload
                    }

                    axios.post(shareServiceUrl + "/share/external", externalShareRequest)
                        .then(response =>{
                            console.log(response);
                            this.externalShareUrl = response.data.url;
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