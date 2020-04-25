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
            var folder = {
                'id': this.containerId
            }
            this.$root.$emit('open-folder', folder);
        }
    }
}