$(document).ready(function () {

    // Default page is 'Home'
    $('#toybox-home-menu').addClass('active');
    $('#toybox-home-container').removeClass('hidden');

    $('.item').click(function () {
        // If the search box is not clicked
        if($(this).attr('class') !== 'ui category search item'){
            // Get all the menuItems and make them inactive
            var menuItems = $('.item');
            for(var i = 0; i < menuItems.length; i++){
                var menuItem = menuItems[i];
                $(menuItem).removeClass('active');
            }
            // Make the clicked menuItem active and get its name
            $(this).addClass('active');
            var menuName = $(this).attr('id').split('-')[1];

            // Make all containers hidden
            var containers = $('.toybox-container');
            for(var i = 0; i < containers.length; i++){
                var container = containers[i];
                if($(container).attr('class').indexOf('hidden') === -1){
                    $(container).addClass('hidden');
                }
            }
            // If the menu name is not signoff then we un-hide the content
            if(menuName !== 'signoff' || menuName !== 'advsearch'){
                $('#toybox-' + menuName + '-container').removeClass('hidden');
            }
        }
    });
});