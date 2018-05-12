var toyboxui;
$(document).ready(function () {

    toyboxui = {
        galleryAssetView: {
            register: function (button, index) {
                var maxNumberOfButtons = 6;
                register(button, index, maxNumberOfButtons, 'GalleryAsset');
            }
        },
        galleryTopMenuView: {
            register: function (button, index) {
                var maxNumberOfButtons = 6;
                register(button, index, maxNumberOfButtons, 'GalleryTopMenu');
            }
        }
        };

    function createButton(button, buttonContainer, index, maxNumberOfButtons, viewName) {
        var existingButtons;
        if(viewName === 'GalleryAsset'){
            existingButtons = buttonContainer.getElementsByClassName('ui icon button');
        }
        else{
            existingButtons = buttonContainer.getElementsByClassName('ui button');
        }

        var dropdownButton = buttonContainer.getElementsByClassName('ui floating dropdown icon button')[0];

        var buttonElement = document.createElement('button');
        if(viewName === 'GalleryAsset'){
            buttonElement.setAttribute('class', button.class);
            buttonElement.innerHTML = button.icon;
        }
        else{
            buttonElement.setAttribute('class', button.secondaryClass);
            buttonElement.innerHTML = button.icon + button.text;
        }



        // TODO:
        // Modify the placement system so that if the buttons are registered in a random way,
        // indexes should still be correct
        if(!index){
            index = 0;
        }

        if(existingButtons.length < maxNumberOfButtons){
            if(index >= buttonContainer.children.length){
                buttonContainer.appendChild(buttonElement);
            }
            else{
                buttonContainer.insertBefore(buttonElement, buttonContainer.children[index]);
            }

            if(viewName === 'GalleryAsset'){
                $(buttonElement).popup({
                    content: button.text
                });
            }

            $(buttonElement).click(function (event) {
                var assetId = $(event.currentTarget.parentNode.parentNode.parentNode).attr('id');
                console.log("Asset ID: " + assetId);
                button.click(assetId);
            });
        }
        else{
            if(!dropdownButton){
                var dropdownDiv = document.createElement('div');
                if(viewName === 'GalleryAsset'){
                    dropdownDiv.setAttribute('class', 'ui floating dropdown icon button');
                }
                else{
                    dropdownDiv.setAttribute('class', 'ui floating right labeled icon dropdown button');
                }


                var icon = document.createElement('i');
                icon.setAttribute('class', 'dropdown icon');
                dropdownDiv.appendChild(icon);

                if(viewName !== 'GalleryAsset'){
                    var span = document.createElement('span');
                    span.setAttribute('class','left text');
                    span.innerHTML = 'More';
                    dropdownDiv.appendChild(span);
                }

                var menuDiv = document.createElement('div');
                menuDiv.setAttribute('class', 'menu');
                dropdownDiv.appendChild(menuDiv);

                buttonContainer.appendChild(dropdownDiv);
            }

            dropdownButton = buttonContainer.getElementsByClassName('ui floating dropdown icon button')[0];

            var item = document.createElement('div');
            item.setAttribute('class','item');
            item.innerHTML = button.icon + button.text;

            var menu = dropdownButton.getElementsByClassName('menu')[0];
            menu.appendChild(item);

            $(item).click(function (event) {
                var assetId = $(event.currentTarget.parentNode.parentNode.parentNode.parentNode.parentNode).attr('id');
                console.log("Asset ID: " + assetId);
                button.click(assetId);
            });

            $('.ui.floating.dropdown.icon.button').dropdown();
        }
    }

    function register(button, index, maxNumberOfButtons, viewName) {
        if(viewName === 'GalleryAsset'){
            registerButtonOnAsset(button, index, maxNumberOfButtons, viewName);
        }
        else if(viewName === 'GalleryTopMenu'){
            registerButtonOnTopMenu(button, index, maxNumberOfButtons, viewName);
        }
    }

    function registerButtonOnTopMenu(button, index, maxNumberOfButtons, viewName){
        var topMenu = $('#gallery-top-menu')[0];
        var buttonContainer = topMenu.getElementsByClassName('ui basic buttons')[0];
        createButton(button, buttonContainer, index, maxNumberOfButtons, viewName);
    }

    function registerButtonOnAsset(button, index, maxNumberOfButtons, viewName) {
        var cards = $('.ui.fluid.card.toybox-card');
        for(var i = 0; i < cards.length; i++){
            var card = cards[i];
            var buttonContainer = card.getElementsByClassName('ui small basic icon buttons')[0];
            createButton(button, buttonContainer, index, maxNumberOfButtons, viewName);
        }
    }
});