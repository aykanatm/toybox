module.exports = {
    mixins:[serviceMixin, userMixin],
    data:function(){
        return{
            componentName: 'Share Modal Window',
            isSharing: false,
            selectionContext: '',
            usersAndUsergroups:[],
            selectedUsers:[],
            selectedUserGroups:[],
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
            enableExpireInternal: false,
            enableExpireExternal: false,
            enableUsageLimit: false,
            externalShareUrl: '',
            externalExpirationDate: '',
            internalExpirationDate: '',
            maxNumberOfHits: -1
        }
    },
    mounted:function(){
        this.$root.$on('open-share-modal-window', (selectionContext) => {
            var shareModalWindow = this;
            setTimeout(() => {
                $('#internal-expiration-date').calendar({
                    type: 'date',
                    formatter: {
                        date: function (date, settings) {
                          if (!date) return '';
                          var day = date.getDate();
                          var month = date.getMonth() + 1;
                          var year = date.getFullYear();
                          return month + '/' + day + '/' + year;
                        }
                    },
                    onChange: function(date, text, mode) {
                        console.log(text);
                        shareModalWindow.internalExpirationDate = text;
                    }
                });
                $('#external-expiration-date').calendar({
                    type: 'date',
                    formatter: {
                        date: function (date, settings) {
                          if (!date) return '';
                          var day = date.getDate();
                          var month = date.getMonth() + 1;
                          var year = date.getFullYear();
                          return month + '/' + day + '/' + year;
                        }
                    },
                    onChange: function(date, text, mode) {
                        shareModalWindow.externalExpirationDate = text;
                    }
                });
            }, 200);

            this.selectedUsers = [];
            this.usersAndUsergroups = [];
            this.selectedUsers = [];
            this.selectedUserGroups = [];

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
            this.enableExpireInternal = false;
            this.enableExpireExternal = false;
            this.enableUsageLimit = false;
            this.externalShareUrl =  '';
            $('#internal-expiration-date-input').attr('disabled', true);
            this.internalExpirationDate = '';
            $('#external-expiration-date-input').attr('disabled', true);
            this.externalExpirationDate = '';
            $('#max-number-of-hits').attr('disabled', true);
            this.maxNumberOfHits = '';

            this.selectionContext = selectionContext;

            this.getService("toybox-user-loadbalancer")
                .then(response => {
                    var userServiceUrl = response.data.value;
                    // Get users
                    axios.get(userServiceUrl + "/users")
                        .then(response => {
                            console.log(response);
                            var users = response.data.users;

                            for(var i = 0; i < users.length; i++){
                                var user = users[i];

                                if(this.user.username !== user.username){
                                    this.usersAndUsergroups.push({
                                        'displayName' : user.name + ' ' + user.lastname + ' (' + user.username + ')',
                                        'id': user.id
                                    });
                                }
                            }

                            $('#user-dropdown').dropdown({
                                onAdd: function(value){
                                    this.addSelectedUsergroupOrUser(value);
                                }.bind(this),
                                onRemove: function(value) {
                                    this.removeSelectedUsergroupOrUser(value);
                                }.bind(this)
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

                    // Get user groups
                    axios.get(userServiceUrl + "/usergroups")
                        .then(response => {
                            console.log(response);
                            var usergroups = response.data.usergroups;

                            for(var i = 0; i < usergroups.length; i++){
                                var usergroup = usergroups[i];

                                this.usersAndUsergroups.push({
                                    'displayName' : usergroup.name,
                                    'id': usergroup.id
                                });
                            }

                            $('#user-dropdown').dropdown({
                                onAdd: function(value){
                                    this.addSelectedUsergroupOrUser(value);
                                }.bind(this),
                                onRemove: function(value) {
                                    this.removeSelectedUsergroupOrUser(value);
                                }.bind(this)
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
            else{
                this.notifyOnView = false;
                this.notifyOnEdit = false;
                this.notifyOnShare = false;
                this.notifyOnMoveOrCopy = false;

                this.canView = true;
                this.canEdit = false;
                this.canDownload = false;
                this.canShare = false;
                this.canMoveOrCopy = false;
            }
        },
        enableExpireInternal:function(value){
            if(value){
                $('#internal-expiration-date-input').attr('disabled', false);
                this.internalExpirationDate = '';
            }
            else{
                $('#internal-expiration-date-input').attr('disabled', true);
                this.internalExpirationDate = '';
            }
        },
        enableExpireExternal:function(value){
            if(value){
                $('#external-expiration-date-input').attr('disabled', false);
                this.externalExpirationDate = '';
            }
            else{
                $('#external-expiration-date-input').attr('disabled', true);
                this.externalExpirationDate = '';
            }
        },
        enableUsageLimit:function(value){
            if(value){
                $('#max-number-of-hits').attr('disabled', false);
                this.maxNumberOfHits = '';
            }
            else{
                $('#max-number-of-hits').attr('disabled', true);
                this.maxNumberOfHits = '';
            }
        }
    },
    methods:{
        generateUrl:function(){
            if(this.enableUsageLimit && this.maxNumberOfHits === ''){
                this.$root.$emit('message-sent', 'Warning', "Usage limit is enabled. Please enter a number for the usage limit.");
            }
            else{
                if(this.enableExpireExternal && this.externalExpirationDate === ''){
                    this.$root.$emit('message-sent', 'Warning', "Expiration date is enabled. Please enter an expiration date.");
                }
                else{
                    this.isSharing = true;
                    this.getService("toybox-share-loadbalancer")
                    .then(response =>{
                        var shareServiceUrl = response.data.value;
                        var externalShareRequest = {
                            selectionContext: this.selectionContext,
                            enableExpireExternal: this.enableExpireExternal,
                            expirationDate: this.externalExpirationDate,
                            enableUsageLimit: this.enableUsageLimit,
                            maxNumberOfHits: this.maxNumberOfHits,
                            notifyWhenDownloaded: this.notifyOnDownload
                        }

                        axios.post(shareServiceUrl + "/share/external", externalShareRequest)
                            .then(response =>{
                                console.log(response);
                                this.isSharing = false;
                                this.externalShareUrl = response.data.url;
                            })
                            .catch(error => {
                                var errorMessage;
                                this.isSharing = false;

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
                        this.isSharing = false;

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
        },
        share:function(){
            if(this.enableExpireInternal && this.internalExpirationDate === ''){
                this.$root.$emit('message-sent', 'Warning', "Expiration date is enabled. Please enter an expiration date.");
            }
            else if(this.selectedUsers.length == 0 && this.selectedUserGroups.length == 0){
                this.$root.$emit('message-sent', 'Warning', "No user or user group is selected. Please select a user or a user group.");
            }
            else{
                this.isSharing = true;
                this.getService("toybox-share-loadbalancer")
                    .then(response =>{
                        var shareServiceUrl = response.data.value;
                        var internalShareRequest = {
                            selectionContext: this.selectionContext,
                            enableExpireInternal: this.enableExpireInternal,
                            expirationDate: this.internalExpirationDate,
                            notifyOnView: this.notifyOnView,
                            notifyOnEdit: this.notifyOnEdit,
                            notifyOnDownload: this.notifyOnDownload,
                            notifyOnShare: this.notifyOnShare,
                            notifyOnMoveOrCopy: this.notifyOnMoveOrCopy,
                            canView: this.canView,
                            canEdit: this.canEdit,
                            canDownload: this.canDownload,
                            canShare: this.canShare,
                            canMoveOrCopy: this.canMoveOrCopy,
                            sharedUsergroups: this.selectedUserGroups,
                            sharedUsers: this.selectedUsers
                        }

                        axios.post(shareServiceUrl + "/share/internal", internalShareRequest)
                            .then(response =>{
                                console.log(response);
                                this.isSharing = false;
                                $(this.$el).modal('hide');
                                this.$root.$emit('message-sent', 'Success', response.data.message);
                            })
                            .catch(error => {
                                var errorMessage;
                                this.isSharing = false;

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
                        this.isSharing = false;

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
        },
        addSelectedUsergroupOrUser:function(value){
            var regExp = /\(([^)]+)\)/;
            var matches = regExp.exec(value);
            if(matches){
                this.selectedUsers.push(matches[1]);
            }
            else{
                this.selectedUserGroups.push(value);
            }
        },
        removeSelectedUsergroupOrUser:function(value){
            var regExp = /\(([^)]+)\)/;
            var matches = regExp.exec(value);
            if(matches){
                this.selectedUsers.splice(matches[1], 1);
            }
            else{
                this.selectedUserGroups.splice(value, 1);
            }
        }
    }
}