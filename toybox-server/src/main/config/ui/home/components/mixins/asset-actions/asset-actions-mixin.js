var assetActionsMixin = {
    methods:{
        downloadAssets(selectedAssetIds){
            this.getConfiguration("assetServiceUrl")
                .then(response =>{
                    if(response){
                        var assets = {
                            selectedAssets: selectedAssetIds
                        }
                        return axios.post(response.data.value + '/assets/download', assets, {responseType:'blob'})
                            .then(response =>{
                                console.log(response);
                                var filename = 'Download.zip';
                                if (typeof window.chrome !== 'undefined') {
                                    // Chrome version
                                    var link = document.createElement('a');
                                    link.href = window.URL.createObjectURL(response.data);
                                    link.download = filename;
                                    document.body.appendChild(link)
                                    link.click();
                                } else if (typeof window.navigator.msSaveBlob !== 'undefined') {
                                    // IE version
                                    var blob = new Blob([response.data], { type: 'application/zip' });
                                    window.navigator.msSaveBlob(blob, filename);
                                } else {
                                    // Firefox version
                                    var file = new File([response.data], filename, { type: 'application/force-download' });
                                    window.open(URL.createObjectURL(file));
                                }
                            })
                            .catch(error => {
                                var errorMessage;

                                if(error.response){
                                    errorMessage = error.response.data.message
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
        downloadAsset(assetName, assetId, mimeType){
            this.getConfiguration("assetServiceUrl")
            .then(response =>{
                if(response){
                    var assets = {
                        selectedAssets: [assetId]
                    }
                    return axios.post(response.data.value + '/assets/download', assets, {responseType:'blob'})
                        .then(response =>{
                            console.log(response);
                            var filename = assetName;
                            if (typeof window.chrome !== 'undefined') {
                                // Chrome version
                                var link = document.createElement('a');
                                link.href = window.URL.createObjectURL(response.data);
                                link.download = filename;
                                document.body.appendChild(link)
                                link.click();
                            } else if (typeof window.navigator.msSaveBlob !== 'undefined') {
                                // IE version
                                var blob = new Blob([response.data], { type: mimeType });
                                window.navigator.msSaveBlob(blob, filename);
                            } else {
                                // Firefox version
                                var file = new File([response.data], filename, { type: 'application/force-download' });
                                window.open(URL.createObjectURL(file));
                            }
                        })
                        .catch(error => {
                            var errorMessage;

                            if(error.response){
                                errorMessage = error.response.data.message
                            }
                            else{
                                errorMessage = error.message;
                            }

                            console.error(errorMessage);
                            this.$root.$emit('message-sent', 'Error', errorMessage);
                        });
                }
            })
        }
    }
}