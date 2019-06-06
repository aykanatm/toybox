module.exports = {
    props:{
        containerId: String,
        containerName: String,
        isActive: Boolean,
        isSection: Boolean
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