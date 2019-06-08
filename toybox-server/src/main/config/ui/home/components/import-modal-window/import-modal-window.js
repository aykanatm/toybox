const STATUS_INITIAL = 0, STATUS_SAVING = 1, STATUS_SUCCESS = 2, STATUS_FAILED = 3;

module.exports = {
    mixins:[serviceMixin],
    data: function() {
        return  {
          componentName: 'Import Modal Window',
          uploadedFiles: [],
          currentStatus: null,
          uploadError: null,
          uploadFieldName: 'upload',
          numberOfFiles: 0,
          containerId:''
        }
    },
    mounted:function(){
        this.$root.$on('open-import-modal-window', (containerId) => {
            this.containerId = containerId;
            this.reset();
            $('#import-modal-window-upload-progress-bar').progress();

            $(this.$el).modal('show');
        });
    },
    computed:{
        isInitial(){
            return this.currentStatus === STATUS_INITIAL;
        },
        isSaving() {
            return this.currentStatus === STATUS_SAVING;
        },
        isSuccess() {
            return this.currentStatus === STATUS_SUCCESS;
        },
        isFailed() {
            return this.currentStatus === STATUS_FAILED;
        }
    },
    methods:{
        reset:function(){
            this.currentStatus = STATUS_INITIAL;
            this.uploadedFiles = [];
            this.uploadError = null;
        },
        save(formData, onProgress){
            this.currentStatus = STATUS_SAVING;

            this.upload(formData, onProgress)
                .then(x => {
                    this.uploadedFiles = [].concat(x);
                    this.currentStatus = STATUS_SUCCESS;
                    this.$refs.fileInputRef.value = '';

                    // Close the modal window
                    $('#toybox-import-modal-window').modal('hide');
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

                    this.uploadError = errorMessage;
                    this.currentStatus = STATUS_FAILED;
                    this.$refs.fileInputRef.value = '';

                    // Close the modal window
                    // Display failure message up top
                    console.error(errorMessage);
                    this.$root.$emit('message-sent', 'Error', errorMessage);
                    $('#toybox-import-modal-window').modal('hide');
                });
        },
        filesChange(fieldName, fileList){
            const formData = new FormData();

            if(!fileList.length){
                return;
            }

            this.numberOfFiles = fileList.length;

            Array
                .from(Array(fileList.length).keys())
                .map(x => {
                    return formData.append(fieldName, fileList[x], fileList[x].name);
                });

            this.save(formData, this.onProgress);
        },
        onProgress(percentCompleted){
            $('#import-modal-window-upload-progress-bar').progress({
                percent: percentCompleted
            });
        },
        upload(formData, onProgress){
            var config = {
                headers: {
                    'container-id': this.containerId
                },
                onUploadProgress(progressEvent) {
                    var percentCompleted = Math.round((progressEvent.loaded * 100) / progressEvent.total);
                    onProgress(percentCompleted);

                    return percentCompleted;
                }
            }

            return this.getService("toybox-asset-loadbalancer")
                .then(response => {
                    if(response){
                        return axios.post(response.data.value + "/assets/upload", formData, config)
                        .then(response =>{
                            this.$root.$emit('message-sent', 'Success', response.data.message);
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
        }
    }
}