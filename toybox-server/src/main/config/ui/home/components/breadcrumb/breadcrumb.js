module.exports = {
    props:{
        containerId: String,
        currentContainerId: String,
        containerName: String,
        isActive: Boolean,
        isSection: Boolean,
        user: Object
    },
    methods:{
        onClick:function(){
            var hasPermission = false;
            var canNavigate = false;
            if(this.containerName === 'Root'){
                if(this.user.isAdmin){
                    hasPermission = true;
                    canNavigate = true;
                }
            }
            else{
                if(this.currentContainerId !== this.containerId){
                    hasPermission = true;
                    canNavigate = false;
                }
            }

            if(hasPermission && canNavigate){
                var folder = {
                    'id': this.containerId
                }

                this.$root.$emit('open-folder', folder);
            }
            else{
                if(!hasPermission){
                    var warning = 'You do not have access to the folder "' + this.containerName + '"';
                    console.warn(warning)
                    this.$root.$emit('message-sent', 'Warning', warning);
                }
            }
        }
    }
}