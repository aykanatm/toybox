var messageMixin = {
    methods:{
        displayMessage:function(type, messageString){
            var typeClass = type === 'Information' ? 'info' : type.toLowerCase()
            $('body')
                .toast({
                    title: type,
                    class: typeClass,
                    message: messageString,
                    position: 'top center',
                    displayTime: 5000
                });
        },
    }
}