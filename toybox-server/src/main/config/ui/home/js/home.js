const home = new Vue({
    el: '#toybox-home',
    data:{
        view: 'home',
        // Messages
        messages: [],
    },
    mounted:function(){
        var csrfHeader = $("meta[name='_csrf_header']").attr("content");
        var csrfToken = $("meta[name='_csrf']").attr("content");
        console.log('CSRF header: ' + csrfHeader);
        console.log('CSRF token: ' + csrfToken);
        axios.defaults.headers = {
            // 'X-CSRF-TOKEN': csrfToken,
            'XSRF-TOKEN': csrfToken
        }
        axios.defaults.withCredentials = true;

        this.$root.$on('message-sent', this.displayMessage);
    },
    methods:{
        displayMessage(type, messageString){
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
        }
    },
    components:{
        'navbar' : httpVueLoader('../components/navbar/navbar.vue'),
        'message' : httpVueLoader('../components/message/message.vue')
    }
});