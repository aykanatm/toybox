const STATUS_INITIAL = 0, STATUS_SAVING = 1, STATUS_SUCCESS = 2, STATUS_FAILED = 3;

module.exports = {
    data: function() {
        return  {
          componentName: 'Import Modal Window',
          uploadedFiles: [],
          currentStatus: null,
          uploadError: null,
          uploadFieldName: 'upload',
          numberOfFiles: 0
        }
    },
    mounted:function(){
        this.reset();
        $('#import-modal-window-upload-progress-bar').progress();
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
                    // TODO:
                    // Close the modal window
                    // Display success message up top

                    // $('#toybox-import-modal-window').modal('hide');
                })
                .catch(err => {
                    var errorMessage = "HTTP " + err.response.data.status + " " + err.response.data.error + ": " + err.response.data.message;
                    console.error(errorMessage);
                    this.uploadError = errorMessage;
                    this.currentStatus = STATUS_FAILED;
                    this.$refs.fileInputRef.value = '';
                    // TODO:
                    // Close the modal window
                    // Display failure message up top

                    // $('#toybox-import-modal-window').modal('hide');
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
                    formData.append(fieldName, fileList[x], fileList[x].name);
                });

            this.save(formData, this.onProgress);
        },
        getConfiguration(fieldName){
            return axios.get("/configuration?field=" + fieldName);
        },
        onProgress(percentCompleted){
            $('#import-modal-window-upload-progress-bar').progress({
                percent: percentCompleted
            });
        },
        upload(formData, onProgress){
            var config = {
                onUploadProgress(progressEvent) {
                    var percentCompleted = Math.round((progressEvent.loaded * 100) / progressEvent.total);
                    onProgress(percentCompleted);

                    return percentCompleted;
                }
            }

            return this.getConfiguration("assetServiceUrl")
                .then(response => {
                    return axios.post(response.data.value + "/upload", formData, config)
                    .then(response => {
                        return this.import(response.data);
                    });
                });
        },
        import(uploadedAssets){
            return this.getConfiguration("jobServiceUrl")
                .then(response => {
                    return axios.post(response.data.value + "/job/import", uploadedAssets);
                });
        }
    }
}