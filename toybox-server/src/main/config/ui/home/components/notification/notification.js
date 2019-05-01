module.exports = {
    mixins:[serviceMixin],
    props:{
        id: Number,
        username: String,
        fromUsername: String,
        notification: String,
        notificationDate: Date,
        isRead: Boolean
    },
    data: function() {
        return  {
          componentName: 'Notification',
          avatarUrl: '',
        }
    },
    mounted: function(){
        this.getService("toybox-rendition-loadbalancer")
            .then(response => {
                var renditionUrl = response.data.value;
                this.avatarUrl = renditionUrl + '/renditions/users/' + this.fromUsername;
            })
            .catch(error => {
                var errorMessage;

                if(error.response.status == 401){
                    window.location = '/logout';
                }
                else{
                    if(error.response){
                        errorMessage = error.response.data.message
                    }
                    else{
                        errorMessage = error.message;
                    }

                    console.error(errorMessage);
                    this.$root.$emit('message-sent', 'Error', errorMessage);
                }
            });
    },
    computed:{
        friendlyDate:function(){
            return moment(this.notificationDate).format('MMMM Do YYYY, hh:mm:ss');
        }
    }
}