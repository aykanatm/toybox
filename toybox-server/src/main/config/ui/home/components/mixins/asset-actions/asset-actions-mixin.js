var assetActionsMixin = {
    methods:{
        deleteAssets(selectedAssets){
            this.getService("toybox-asset-loadbalancer")
                .then(response => {
                    if(response){
                        var assets = {
                            'selectedAssets': selectedAssets
                        }
                        return axios.post(response.data.value + '/assets/delete', assets)
                            .then(response => {
                                console.log(response);
                                this.$root.$emit('message-sent', 'Success', response.data.message);
                                this.$root.$emit('refresh-assets');
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
        },
        subscribeToAssets(selectedAssets){
            this.getService("toybox-asset-loadbalancer")
                .then(response =>{
                    if(response){
                        var assets = {
                            'selectedAssets': selectedAssets
                        }

                        return axios.post(response.data.value + '/assets/subscribe', assets)
                            .then(response => {
                                console.log(response);
                                this.$root.$emit('message-sent', 'Success', response.data.message);
                                this.$root.$emit('refresh-assets');
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
        },
        unsubscribeFromAssets(selectedAssets){
            this.getService("toybox-asset-loadbalancer")
            .then(response =>{
                if(response){
                    var assets = {
                        'selectedAssets': selectedAssets
                    }

                    return axios.post(response.data.value + '/assets/unsubscribe', assets)
                        .then(response => {
                            console.log(response);
                            this.$root.$emit('message-sent', 'Success', response.data.message);
                            this.$root.$emit('refresh-assets');
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
        },
        downloadAssets(selectedAssets){
            this.getService("toybox-asset-loadbalancer")
                .then(response =>{
                    if(response){
                        var assets = {
                            'selectedAssets': selectedAssets
                        }
                        return axios.post(response.data.value + '/assets/download', assets, {responseType:'blob'})
                            .then(response =>{
                                console.log(response);
                                var filename;
                                var mimeType;

                                if(assets.selectedAssets.length == 1){
                                    filename = assets.selectedAssets[0].name;
                                    mimeType = assets.selectedAssets[0].type;
                                }
                                else{
                                    filename = 'Download.zip';
                                    mimeType = 'application/zip';
                                }

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
                });
        }
    }
}