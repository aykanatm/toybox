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
                    var container ={
                        '@class': 'com.github.murataykanat.toybox.dbo.Container',
                    }

                    var searchRequest = {
                        'limit': -1,
                        'offset': 0,
                        'container': container
                    }

                    var containerServiceUrl = response.data.value

                    axios.post(containerServiceUrl + "/containers/search", searchRequest)
                        .then(response => {
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
            var assetIds = [];
            for(var i = 0; i < this.selectedAssets.length; i++){
                console.log(this.selectedAssets[i]);
                assetIds.push(this.selectedAssets[i].id);
            }
            console.log('To the following folders:')
            var containerIds = [];
            for(var i = 0; i < selectedFolders.length; i++){
                console.log(selectedFolders[i]);
                containerIds.push(selectedFolders[i].key);
            }

            this.getService("toybox-asset-loadbalancer")
                .then(response =>{
                    var assetServiceUrl = response.data.value;

                    var copyRequest = {
                        'assetIds': assetIds,
                        'containerIds': containerIds
                    }

                    axios.post(assetServiceUrl + "/assets/copy", copyRequest)
                    .then(response =>{
                        this.$root.$emit('message-sent', 'Success', response.data.message);
                        this.$root.$emit('refresh-assets');
                        this.$root.$emit('refresh-items');
                        $(this.$el).modal('hide');
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
                    var commonObjectsServiceUrl = response.data.value;

                    var moveRequest = {
                        'selectionContext': this.selectionContext,
                        'containerId': selectedFolders[0].key
                    }

                    axios.post(commonObjectsServiceUrl + "/common-objects/move", moveRequest)
                    .then(response =>{
                        this.$root.$emit('message-sent', 'Success', response.data.message);
                        this.$root.$emit('refresh-assets');
                        this.$root.$emit('refresh-items');
                        $(this.$el).modal('hide');
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
    },
    components:{

    }
}