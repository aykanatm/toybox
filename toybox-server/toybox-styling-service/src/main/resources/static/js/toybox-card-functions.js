$(document).ready(function () {

    var cards = $('.ui .card');
    var maxButtonCountOnCard = 7;

    for(var i = 0; i < cards.length; i++){
        card = cards[i];

        var iconButtons = card.getElementsByClassName('ui icon button');
        var menu = card.getElementsByClassName('menu')[0];

        // If there are more than maximum number of allowed buttons on a card
        if(iconButtons.length > maxButtonCountOnCard){
            var startIndex = maxButtonCountOnCard - 1;
            var buttonsToRemove = [];

            for(var j = startIndex; j < iconButtons.length; j++){
                var button = iconButtons[j];
                var buttonClass = $(button).attr('class');
                var buttonName = buttonClass.split(' ')[3];

                if(buttonName.indexOf('toybox') != -1){
                    var item = document.createElement('div');
                    item.setAttribute('class','item ' + buttonName);
                    // item.setAttribute('id', buttonId);
                    item.innerHTML = iconButtons[j].innerHTML;
                    menu.appendChild(item);

                    buttonsToRemove.push(button);
                }
            }

            // Remove the buttons
            for(var j = 0; j < buttonsToRemove.length; j++){
                var button = buttonsToRemove[j];
                button.parentNode.removeChild(button);
            }
        }
        else{
            // If there are less than maximum number of allowed buttons remove the dropdown
            var iconDropdown = card.getElementsByClassName('ui floating dropdown icon button')[0];
            iconDropdown.parentNode.removeChild(iconDropdown);
        }
    }

    setTimeout(function () {
        // Initialize all dropdowns
        $('.ui.dropdown')
            .dropdown()
        ;

        // Set tooltips
        $('.toybox-button-share').popup({
            content: 'Share'
        });

        $('.toybox-button-download').popup({
            content: 'Download'
        });

        $('.toybox-button-rename').popup({
            content: 'Rename'
        });

        $('.toybox-button-copy').popup({
            content: 'Copy'
        });

        $('.toybox-button-move').popup({
            content: 'Move'
        });

        $('.toybox-button-subsribe').popup({
            content: 'Subscribe'
        });

        $('.toybox-button-delete').popup({
            content: 'Delete'
        });

        $('.toybox-button-versionhistory').popup({
            content: 'Version History'
        });
    }, 200);
});
