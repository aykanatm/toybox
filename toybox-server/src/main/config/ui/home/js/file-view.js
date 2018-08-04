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
});