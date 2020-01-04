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
            notifyOnEdit: false,
            notifyOnDownload: false,
            notifyOnShare: false,
            notifyOnCopy: false,
            // Share permissions
            initialCanEdit: false,
            initialCanDownload: false,
            initialCanShare: false,
            initialCanCopy: false,
            canEdit: false,
            canDownload: false,
            canShare: false,
            canCopy: false,
            hasSharedItem: false,
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
        this.$root.$on('open-share-modal-window', (selectionContext, type, id) => {
            var shareModalWindow = this;
            this.isSharing = false;

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

            this.usersAndUsergroups = [];
            this.selectedUsers = [];
            this.selectedUserGroups = [];

            this.isExternalUser = false;
            this.notifyMe = false;
            this.notifyOnEdit = false;
            this.notifyOnDownload = false;
            this.notifyOnShare = false;
            this.notifyOnCopy = false;
            this.canEdit = false;
            this.canDownload = false;
            this.canShare = false;
            this.canCopy = false;
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


            $('#user-dropdown').dropdown({
                onAdd: function(value){
                    this.addSelectedUsergroupOrUser(value);
                }.bind(this),
                onRemove: function(value) {
                    this.removeSelectedUsergroupOrUser(value);
                }.bind(this)
            });

            $('#user-dropdown').dropdown('clear');

            if(!selectionContext && type && id){
                this.getService("toybox-share-loadbalancer")
                    .then(response => {
                        if(response){
                            var shareServiceUrl = response.data.value;
                            axios.get(shareServiceUrl + "/share/" + id + "?type=" + type)
                                .then(response =>{
                                    if(response){
                                        console.log(response);
                                        this.populateWindow(response.data.selectionContext, response.data.shareItem);
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
            }
            else{
                this.populateWindow(selectionContext, undefined);
            }

            $(this.$el).modal('setting', 'closable', false).modal('show');
        });
    },
    watch:{
        notifyMe:function(value){
            if(!value){
                this.notifyOnEdit = false;
                this.notifyOnDownload = false;
                this.notifyOnShare = false;
                this.notifyOnCopy = false;
            }
        },
        isExternalUser:function(value){
            if(value){
                this.notifyOnEdit = false;
                this.notifyOnShare = false;
                this.notifyOnCopy = false;

                this.canEdit = false;
                this.canDownload = true;
                this.canShare = false;
                this.canCopy = false;
            }
            else{
                this.notifyOnEdit = false;
                this.notifyOnShare = false;
                this.notifyOnCopy = false;

                this.canEdit = this.initialCanCopy;
                this.canDownload = this.initialCanDownload;
                this.canShare = this.initialCanShare;
                this.canCopy = this.initialCanCopy;
            }
        },
        enableExpireInternal:function(value){
            if(value){
                $('#internal-expiration-date-input').attr('disabled', false);
            }
            else{
                $('#internal-expiration-date-input').attr('disabled', true);
                this.internalExpirationDate = '';
            }
        },
        enableExpireExternal:function(value){
            if(value){
                $('#external-expiration-date-input').attr('disabled', false);
            }
            else{
                $('#external-expiration-date-input').attr('disabled', true);
                this.externalExpirationDate = '';
            }
        },
        enableUsageLimit:function(value){
            if(value){
                $('#max-number-of-hits').attr('disabled', false);
            }
            else{
                $('#max-number-of-hits').attr('disabled', true);
                this.maxNumberOfHits = '';
            }
        }
    },
    methods:{
        populateWindow:function(selectionContext, share){
            this.selectionContext = selectionContext;

            if(!share){
                var canEditAll = true;
                var canDownloadAll = true;
                var canShareAll = true;
                var canCopyAll = true;
                this.hasSharedItem = false;

                for(var j = 0; j < this.selectionContext.selectedAssets.length; j++){
                    var selectedAsset = this.selectionContext.selectedAssets[j];
                    if(selectedAsset.shared === 'Y'){
                        canEditAll = canEditAll && (selectedAsset.canEdit === 'Y');
                        canDownloadAll = canDownloadAll && (selectedAsset.canDownload === 'Y');
                        canShareAll = canShareAll && (selectedAsset.canShare === 'Y');
                        canCopyAll = canCopyAll && (selectedAsset.canCopy === 'Y');
                        this.hasSharedItem = true;
                    }
                }

                for(var j = 0; j < this.selectionContext.selectedContainers.length; j++){
                    var selectedContainer = this.selectionContext.selectedContainers[j];
                    if(selectedContainer.shared === "Y"){
                        canEditAll = canEditAll && (selectedContainer.canEdit === 'Y');
                        canDownloadAll = canDownloadAll && (selectedContainer.canDownload === 'Y');
                        canShareAll = canShareAll && (selectedContainer.canShare === 'Y');
                        canCopyAll = canCopyAll && (selectedContainer.canCopy === 'Y');
                        this.hasSharedItem = true;
                    }
                }

                if(this.hasSharedItem){
                    this.canEdit = canEditAll;
                    this.canDownload = canDownloadAll;
                    this.canShare = canShareAll;
                    this.canCopy = canCopyAll;
                }

                this.initialCanEdit = this.canEdit;
                this.initialCanDownload = this.canDownload;
                this.initialCanShare = this.canShare;
                this.initialCanCopy = this.canCopy;
            }
            else{
                this.canEdit = share.canEdit;
                this.canDownload = share.canDownload;
                this.canShare = share.canShare;
                this.canCopy = share.canCopy;

                this.notifyMe = share.notifyOnCopy || share.notifyOnDownload || share.notifyOnEdit || share.notifyOnShare;
                this.notifyOnCopy = share.notifyOnCopy;
                this.notifyOnDownload = share.notifyOnDownload;
                this.notifyOnEdit = share.notifyOnEdit;
                this.notifyOnShare = share.notifyOnShare;

                this.isExternalUser = share['@class'] === 'com.github.murataykanat.toybox.dbo.ExternalShare';
                $('.user-selection input').attr('disabled', true);
                this.externalShareUrl =  share.url;

                if(share.enableExpire === 'Y'){
                    if(this.isExternalUser){
                        this.enableExpireExternal = true;
                        $('#external-expiration-date-input').val(convertToToyboxDateString(share.expirationDate));
                    }
                    else{
                        this.enableExpireInternal = true;
                        $('#internal-expiration-date-input').val(convertToToyboxDateString(share.expirationDate));
                    }
                }
                else{
                    if(this.isExternalUser){
                        this.enableExpireExternal = false;
                        $('#external-expiration-date-input').attr('disabled', true);
                    }
                    else{
                        this.enableExpireInternal = false;
                        $('#internal-expiration-date-input').attr('disabled', true);
                    }
                }

                if(share.enableUsageLimit === 'Y'){
                    this.enableUsageLimit = true;
                    this.maxNumberOfHits = share.maxNumberOfHits;
                }
                else{
                    this.enableUsageLimit = false;
                    $('#max-number-of-hits').attr('disabled', true);
                }
            }

            if(this.selectionContext){
                this.getService("toybox-user-loadbalancer")
                .then(response => {
                    if(response){
                        var userServiceUrl = response.data.value;
                        // Get users
                        axios.get(userServiceUrl + "/users")
                            .then(response => {
                                if(response){
                                    console.log(response);
                                    var users = response.data.users;

                                    for(var i = 0; i < users.length; i++){
                                        var user = users[i];

                                        var isSelf = this.user.username === user.username;
                                        var isOriginalSharer = false;

                                        for(var j = 0; j < this.selectionContext.selectedAssets.length; j++){
                                            var selectedAsset = this.selectionContext.selectedAssets[j];
                                            if(selectedAsset.shared === "Y" && selectedAsset.sharedByUsername === user.username){
                                                isOriginalSharer = true;
                                                break;
                                            }
                                        }

                                        if(!isOriginalSharer){
                                            for(var j = 0; j < this.selectionContext.selectedContainers.length; j++){
                                                var selectedContainer = this.selectionContext.selectedContainers[j];
                                                if(selectedContainer.shared === "Y" && selectedContainer.sharedByUsername === user.username){
                                                    isOriginalSharer = true;
                                                    break;
                                                }
                                            }
                                        }

                                        if(!isSelf && !isOriginalSharer){
                                            this.usersAndUsergroups.push({
                                                'displayName' : user.name + ' ' + user.lastname + ' (' + user.username + ')',
                                                'id': user.id
                                            });
                                        }
                                    }

                                    if(share){
                                        setTimeout(() => {
                                            var selectedArray = [];
                                            var shareUsers = share.users;
                                            for(var i = 0; i < shareUsers.length; i++){
                                                var shareUser = shareUsers[i];
                                                var userDisplayName = shareUser.name + ' ' + shareUser.lastname + ' (' + shareUser.username + ')';
                                                selectedArray.push(userDisplayName);
                                                this.selectedUsers.push(shareUser.username);
                                            }

                                            $('.ui.fluid.dropdown').dropdown('set selected', selectedArray);
                                        }, 200);
                                    }
                                }
                                else{
                                    this.$root.$emit('message-sent', 'Error', "There was no response from the user loadbalancer!");
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

                        // Get user groups
                        axios.get(userServiceUrl + "/usergroups")
                            .then(response => {
                                if(response){
                                    console.log(response);
                                    var usergroups = response.data.usergroups;

                                    for(var i = 0; i < usergroups.length; i++){
                                        var usergroup = usergroups[i];

                                        this.usersAndUsergroups.push({
                                            'displayName' : usergroup.name,
                                            'id': usergroup.id
                                        });
                                    }
                                }
                                else{
                                    this.$root.$emit('message-sent', 'Error', "There was no response from the user loadbalancer!");
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
            }
        },
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
                        if(response){
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
                                    if(response){
                                        console.log(response);
                                        this.isSharing = false;
                                        this.externalShareUrl = response.data.url;
                                    }
                                    else{
                                        this.isSharing = false;
                                        this.$root.$emit('message-sent', 'Error', "There was no response from the share loadbalancer!");
                                    }
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
                        else{
                            this.$root.$emit('message-sent', 'Error', "There was no response from the service endpoint!");
                        }
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
                        if(response){
                            var shareServiceUrl = response.data.value;
                            var internalShareRequest = {
                                selectionContext: this.selectionContext,
                                enableExpireInternal: this.enableExpireInternal,
                                expirationDate: this.internalExpirationDate,
                                notifyOnEdit: this.notifyOnEdit,
                                notifyOnDownload: this.notifyOnDownload,
                                notifyOnShare: this.notifyOnShare,
                                notifyOnCopy: this.notifyOnCopy,
                                canEdit: this.canEdit,
                                canDownload: this.canDownload,
                                canShare: this.canShare,
                                canCopy: this.canCopy,
                                sharedUsergroups: this.selectedUserGroups,
                                sharedUsers: this.selectedUsers
                            }

                            axios.post(shareServiceUrl + "/share/internal", internalShareRequest)
                                .then(response =>{
                                    if(response){
                                        console.log(response);
                                        this.isSharing = false;
                                        $(this.$el).modal('hide');
                                        this.$root.$emit('message-sent', 'Success', response.data.message);
                                    }
                                    else{
                                        this.isSharing = false;
                                        this.$root.$emit('message-sent', 'Error', "There was no response from the share loadbalancer!");
                                    }
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
                        else{
                            this.isSharing = false;
                            this.$root.$emit('message-sent', 'Error', "There was no response from the service endpoint!");
                        }
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