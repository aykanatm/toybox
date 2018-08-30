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
        save(formData){
            this.currentStatus = STATUS_SAVING;

            this.upload(formData)
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
                    this.uploadError = err.response;
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

            this.save(formData);
        },
        upload(formData){
            var uploadUrl = 'http://localhost:8101/upload';
            return axios.post(uploadUrl, formData).then(x => x.data);
        }
    }
}