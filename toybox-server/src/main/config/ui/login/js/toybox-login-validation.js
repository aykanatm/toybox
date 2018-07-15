$(document).ready(function () {
    $('.ui.form')
        .form({
            fields: {
                name: {
                    identifier: 'username',
                    rules: [
                        {
                            type   : 'empty',
                            prompt : 'Please provide a username.'
                        }
                    ]
                },
                skills: {
                    identifier: 'password',
                    rules: [
                        {
                            type   : 'empty',
                            prompt : 'Please provide a password.'
                        }
                    ]
                },
            }
        });
});
