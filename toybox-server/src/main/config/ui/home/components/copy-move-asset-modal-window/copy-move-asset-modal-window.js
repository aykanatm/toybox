module.exports = {
    mixins:[serviceMixin, userMixin],
    data:function(){
        return{
            componentName: 'Copy Move Asset Modal Window',
            selectionContext:'',
            isMove: false,
            isCopy: false,
            hasSelectedFolders: false
        }
    },
    mounted:function(){
        var self = this;
        this.$root.$on('open-copy-move-asset-modal-window', (selectionContext, isMove, isCopy) => {
            this.selectionContext = selectionContext;
            this.isMove = isMove;
            this.isCopy = isCopy;

            this.getService("toybox-folder-loadbalancer")
                .then(response => {
                    if(response){
                        var container ={
                            '@class': 'com.github.murataykanat.toybox.dbo.Container',
                        }

                        var searchRequest = {
                            'limit': -1,
                            'offset': 0,
                            'container': container,
                            'retrieveTopLevelContainers': 'N'
                        }

                        var containerServiceUrl = response.data.value

                        axios.post(containerServiceUrl + "/containers/search", searchRequest)
                            .then(response => {
                                if(response){
                                    console.log(response);
                                    var containers = response.data.containers;
                                    var folderSource = this.getContainerTree(containers);

                                    if($('#folder-tree')[0].children.length == 0){
                                        $('#folder-tree').fancytree({
                                            extensions: ["glyph"],
                                            glyph: {
                                                    preset: "bootstrap3",
                                                    map: {
                                                        doc: "icon-folder",
                                                        docOpen: "icon-folder",
                                                        checkbox: "icon-check-empty",
                                                        checkboxSelected: "icon-check",
                                                        checkboxUnknown: "minus square outline icon",
                                                        dragHelper: "play icon",
                                                        dropMarker: "icon-right-open",
                                                        error: "exclamation circle icon",
                                                        expanderClosed: "icon-right-open",
                                                        expanderLazy: "icon-right-open",
                                                        expanderOpen: "icon-down-open",
                                                        folder: "icon-folder",
                                                        folderOpen: "icon-folder-open",
                                                        loading: "hourglass outline icon",
                                                }
                                            },
                                            checkbox: true,
                                            selectMode: (isMove? 1 : 2),
                                            source: folderSource,
                                            click: function(event, data){
                                                setTimeout(() => {
                                                    self.hasSelectedFolders = $('#folder-tree').fancytree('getTree').getSelectedNodes().length > 0;
                                                }, 200);
                                              }
                                        });

                                        $("#folder-tree").fancytree("getTree").visit(function(node) {
                                            node.setExpanded(true);
                                        });
                                    }
                                    else{
                                        var folderTree = $('#folder-tree').fancytree('getTree');
                                        folderTree.reload(folderSource);

                                        $("#folder-tree").fancytree("getTree").visit(function(node) {
                                            node.setExpanded(true);
                                        });

                                        $("#folder-tree").fancytree("option", "selectMode", (isMove? 1 : 2));
                                    }

                                    this.hasSelectedFolders = false;
                                }
                                else{
                                    this.isLoading = false;
                                    this.$root.$emit('message-sent', 'Error', "There was no response from the folder loadbalancer!");
                                }
                            })
                            .catch(error => {
                                this.isLoading = false;
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
                        this.isLoading = false;
                        this.$root.$emit('message-sent', 'Error', "There was no response from the service endpoint!");
                    }
                });

            $(this.$el).modal('setting', 'closable', false).modal('show');
        });
    },
    methods:{
        getContainerTree(containers){
            var folderSource = [];

            for(var i = 0; i < containers.length; i++){
                var container = containers[i];
                if(!container.parentId){
                    folderSource.push({title: container.name, key: container.id, folder: true, children: this.getChildContainers(containers, container.id)});
                }
            }

            return folderSource
        },
        getChildContainers(containers, containerId){
            var children = [];
            for(var i = 0; i < containers.length; i++){
                var container = containers[i]
                if(container.parentId === containerId){
                    children.push({title: container.name, key: container.id, folder: true, children: this.getChildContainers(containers, container.id)});
                }
            }

            return children;
        },
        copyAsset(){
            var selectedFolders = $('#folder-tree').fancytree('getTree').getSelectedNodes();

            console.log('Copying the following assets:');
            var containerIds = [];
            for(var i = 0; i < selectedFolders.length; i++){
                console.log(selectedFolders[i]);
                containerIds.push(selectedFolders[i].key);
            }

            this.getService("toybox-common-object-loadbalancer")
                .then(response =>{
                    if(response){
                        var assetServiceUrl = response.data.value;

                        var copyRequest = {
                            'selectionContext': this.selectionContext,
                            'containerIds': containerIds
                        }

                        axios.post(assetServiceUrl + "/common-objects/copy", copyRequest)
                        .then(response =>{
                            if(response){
                                this.$root.$emit('message-sent', 'Success', response.data.message);
                                this.$root.$emit('refresh-assets');
                                this.$root.$emit('refresh-items');
                                $(this.$el).modal('hide');
                            }
                            else{
                                this.$root.$emit('message-sent', 'Error', "There was no response from the common object loadbalancer!");
                            }
                        })
                        .catch(error => {
                            this.isLoading = false;
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
                    this.isLoading = false;
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
        moveAsset(){
            var selectedFolders = $('#folder-tree').fancytree('getTree').getSelectedNodes();

            this.getService("toybox-common-object-loadbalancer")
                .then(response =>{
                    if(response){
                        var commonObjectsServiceUrl = response.data.value;

                        var moveRequest = {
                            'selectionContext': this.selectionContext,
                            'containerId': selectedFolders[0].key
                        }

                        axios.post(commonObjectsServiceUrl + "/common-objects/move", moveRequest)
                        .then(response =>{
                            if(response){
                                this.$root.$emit('message-sent', 'Success', response.data.message);
                                this.$root.$emit('refresh-assets');
                                this.$root.$emit('refresh-items');
                                $(this.$el).modal('hide');
                            }
                            else{
                                this.$root.$emit('message-sent', 'Error', "There was no response from the common object loadbalancer!");
                            }
                        })
                        .catch(error => {
                            this.isLoading = false;
                            var errorMessage;

                            if(error.response){
                                errorMessage = error.response.data.message
                                if(error.response.status == 304){
                                    errorMessage = 'No asset or folder is moved because either the target folder is the same as the selected folders or the target folder is a sub folder of the selected folders.';
                                }
                                else if(error.response.status == 401){
                                    window.location = '/logout';
                                }
                            }
                            else{
                                errorMessage = error.message;
                            }


                            if(error.response.status == 304){
                                console.warn(errorMessage);
                                this.$root.$emit('message-sent', 'Warning', errorMessage);
                                this.$root.$emit('refresh-assets');
                                this.$root.$emit('refresh-items');
                                $(this.$el).modal('hide');
                            }
                            else{
                                console.error(errorMessage);
                                this.$root.$emit('message-sent', 'Error', errorMessage);
                            }
                        });
                    }
                    else{
                        this.$root.$emit('message-sent', 'Error', "There was no response from the service endpoint!");
                    }
                })
                .catch(error => {
                    this.isLoading = false;
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