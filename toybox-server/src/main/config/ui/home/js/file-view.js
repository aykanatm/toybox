$(document).ready(function () {
    var cards = $('.toybox-card');
    var gridListToggleButton = $('#grid-list-toggle-button');

    gridListToggleButton.click(function () {
        var isGrid = gridListToggleButton[0].innerHTML.indexOf('grid') !== - 1;
        if(isGrid){
            gridListToggleButton[0].innerHTML = '<i class="list icon"></i>';
        }
        else{
            gridListToggleButton[0].innerHTML = '<i class="grid layout icon"></i>';
        }
    });

    cards.click(function () {
       var cardId = $(this).attr('id');
       var cardClass = $(this).attr('class');
       if(cardClass.indexOf('toybox-card-selected') !== -1){
           $(this).removeClass('toybox-card-selected');
           var cornerLabel = $(this)[0].getElementsByClassName('toybox-corner-label')[0];
           $(cornerLabel).addClass('hidden');
           console.log("Asset with ID " + cardId + ' de-selected.');
       }
       else{
           $(this).addClass('toybox-card-selected');
           var cornerLabel = $(this)[0].getElementsByClassName('toybox-corner-label')[0];
           $(cornerLabel).removeClass('hidden');
           console.log("Asset with ID " + cardId + ' selected.');
       }
    });
});