module.exports = {
    props:{
        id: Number,
        isError: Boolean,
        isOk: Boolean,
        isWarning: Boolean,
        isInfo: Boolean,
        header: String,
        message: String
    },
    computed:{

    },
    data:function(){
        return{
            componentName: 'Message',
        }
    }
}