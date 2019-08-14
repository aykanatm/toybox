var itemActionsMixin = {
    methods:{
        deleteItems(selectedItems){

        },
        subscribeToItems(selectedItems){

        },
        unsubscribeFromItems(selectedItems){

        },
        downloadItems(selectedItems){
            console.log(selectedItems);
            var selectedAssets = [];
            var selectedContainers = [];

            for(var i = 0; i < selectedItems.length; i++){
                var selectedItem = selectedItems[i];
                if(selectedItem['@class'] === 'com.github.murataykanat.toybox.dbo.Asset'){
                    selectedAssets.push(selectedItem);
                }
                else{
                    selectedContainers.push(selectedItem);
                }
            }

            this.getService("toybox-common-object-loadbalancer")
            .then(response =>{
                if(response){
                    var selectionContext = {
                        'selectedAssets': selectedAssets,
                        'selectedContainers': selectedContainers
                    }
                    return axios.post(response.data.value + '/common-objects/download', selectionContext, {responseType:'blob'})
                        .then(response =>{
                            console.log(response);
                            var filename = 'Download.zip';
                            var blob = new Blob([response.data], {type:'application/octet-stream'});
                            saveAs(blob , filename);
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
            });
        },
        renameItem(itemId, itemName, itemType){

        },
        showVersionHistoryOfItem(itemId, thumbnailUrl){

        }
    }
}