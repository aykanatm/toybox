var messageMixin = {
    data:function(){
        return{
            messages: [],
        }
    },
    methods:{
        displayMessage:function(type, messageString){
            var isError = false;
            var isOk = false;
            var isWarning = false;
            var isInfo = false;

            if(type === 'Information'){
                isInfo = true;
            }
            else if(type === 'Error'){
                isError = true;
            }
            else if(type === 'Warning'){
                isWarning = true;
            }
            else if(type === 'Success'){
                isOk = true;
            }
            else{
                isInfo = true;
            }

            var message = {id:this.messages.length, isError:isError, isOk:isOk, isWarning:isWarning, isInfo:isInfo, header:type, message:messageString}
            this.messages.push(message);
            // Initialize events on the message
            setTimeout(() => {
                $('.message .close')
                    .on('click', function() {
                        $(this)
                        .closest('.message')
                        .remove();
                    });

                var messageSelector = '#' + message.id + '.message';
                $(messageSelector)
                    .delay(2000)
                    .queue(function(){
                        $(this).remove().dequeue();
                    });
            }, 100);
        },
    }
}