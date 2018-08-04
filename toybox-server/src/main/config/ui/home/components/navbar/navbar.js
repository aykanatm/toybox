module.exports = {
    data: function() {
        return  {
          componentName: 'Navigation Bar'
        }
    },
    mounted: function(){
        $('#toybox-home-menu').popup({
            content: 'Home'
        });

        $('#toybox-files-menu').popup({
            content: 'Files'
        });

        $('#toybox-folders-menu').popup({
            content: 'Folders'
        });

        $('#toybox-advsearch-menu').popup({
            content: 'Advanced Search'
        });

        $('#toybox-profile-menu').popup({
            content: 'Profile'
        });

        $('#toybox-help-menu').popup({
            content: 'Help'
        });

        $('#toybox-signoff-menu').popup({
            content: 'Sign Off'
        });
    },
}