$(document).ready(function () {
    var maxNumberOfButtonsOnAsset = 6;
    function register(button, index) {
        var cards = $('.ui.fluid.card.toybox-card');
        for(var i = 0; i < cards.length; i++){
            var card = cards[i];
            var buttonContainer = card.getElementsByClassName('ui small basic icon buttons')[0];
            var existingButtons = buttonContainer.getElementsByClassName('ui icon button');
            var dropdownButton = buttonContainer.getElementsByClassName('ui floating dropdown icon button')[0];

            var buttonElement = document.createElement('button');
            buttonElement.setAttribute('class', button.class);
            buttonElement.innerHTML = button.icon;

            // TODO:
            // Modify the placement system so that if the buttons are registered in a random way,
            // indexes should still be correct
            if(!index){
                index = 0;
            }

            if(existingButtons.length < maxNumberOfButtonsOnAsset){
                if(index >= buttonContainer.children.length){
                    buttonContainer.appendChild(buttonElement);
                }
                else{
                    buttonContainer.insertBefore(buttonElement, buttonContainer.children[index]);
                }

                $(buttonElement).popup({
                    content: button.text
                });

                $(buttonElement).click(function (event) {
                    var assetId = $(event.currentTarget.parentNode.parentNode.parentNode).attr('id');
                    console.log("Asset ID: " + assetId);
                    button.click(assetId);
                });
            }
            else{
                if(!dropdownButton){
                    var dropdownDiv = document.createElement('div');
                    dropdownDiv.setAttribute('class', 'ui floating dropdown icon button');

                    var icon = document.createElement('i');
                    icon.setAttribute('class', 'dropdown icon');
                    dropdownDiv.appendChild(icon);

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
    }

    // TODO:
    // Pass selection context rather than the asset id
    function btnShare(assetId) {
        console.log('Sharing the file with ID "' + assetId + '"');
    }

    function btnShareSetup() {
        // TODO:
        // Check permission
        return true;
    }

    function btnDownload(assetId) {
        console.log('Downloading the file with ID "' + assetId + '"');
    }

    function btnDownloadSetup() {
        // TODO:
        // Check permission
        return true;
    }

    function btnRename(assetId) {
        console.log('Renaming the file with ID "' + assetId + '"');
    }

    function btnRenameSetup() {
        // TODO:
        // Check permission
        return true;
    }

    function btnCopy(assetId) {
        console.log('Opening copy modal window for asset with ID "' + assetId + '"');
    }

    function btnCopySetup() {
        // TODO:
        // Check permission
        return true;
    }

    function btnMove(assetId) {
        console.log('Opening move modal window for asset with ID "' + assetId + '"');
    }

    function btnMoveSetup() {
        // TODO:
        // Check permission
        return true;
    }

    function btnSubscribe(assetId) {
        console.log('Subscribing to the asset with ID "' + assetId + '"');
    }

    function btnSubscribeSetup() {
        // TODO:
        // Check permission
        return true;
    }

    function btnDelete(assetId) {
        console.log('Deleting asset with ID "' + assetId + '"');
    }

    function btnDeleteSetup() {
        // TODO:
        // Check permission
        return true;
    }

    function btnShowVersionHistory(assetId) {
        console.log('Showing version history of asset with ID "' + assetId + '"');
    }

    function btnShowVersionHistorySetup() {
        // TODO:
        // Check permission
        return true;
    }

    var shareButton = {
        class : 'ui icon button',
        text : 'Share',
        icon : '<i class="share alternate icon"></i>',
        setup : function setup() {
            btnShareSetup();
        },
        click : function click(assetId){
            btnShare(assetId);
        }
    };

    var downloadButton = {
        class : 'ui icon button',
        text : 'Download',
        icon : '<i class="download icon"></i>',
        setup : function setup() {
            btnDownloadSetup();
        },
        click : function click(assetId){
            btnDownload(assetId);
        }
    };

    var renameButton = {
        class : 'ui icon button',
        text : 'Rename',
        icon : '<i class="i cursor icon"></i>',
        setup : function setup() {
            btnRenameSetup();
        },
        click : function click(assetId){
            btnRename(assetId);
        }
    };

    var copyButton = {
        class : 'ui icon button',
        text : 'Copy',
        icon : '<i class="copy icon"></i>',
        setup : function setup() {
            btnCopySetup();
        },
        click : function click(assetId){
            btnCopy(assetId);
        }
    };

    var moveButton = {
        class : 'ui icon button',
        text : 'Move',
        icon : '<i class="external alternate icon"></i>',
        setup : function setup() {
            btnMoveSetup();
        },
        click : function click(assetId){
            btnMove(assetId);
        }
    };

    var subscribeButton = {
        class : 'ui icon button',
        text : 'Subscribe',
        icon : '<i class="rss icon"></i>',
        setup : function setup() {
            btnSubscribeSetup();
        },
        click : function click(assetId){
            btnSubscribe(assetId);
        }
    };

    var deleteButton = {
        class : 'ui icon button',
        text : 'Delete',
        icon : '<i class="trash alternate icon"></i>',
        setup : function setup() {
            btnDeleteSetup();
        },
        click : function click(assetId){
            btnDelete(assetId);
        }
    };

    var showVersionHistoryButton = {
        class : 'ui icon button',
        text : 'Version History',
        icon : '<i class="list alternate outline icon"></i>',
        setup : function setup() {
            btnShowVersionHistorySetup();
        },
        click : function click(assetId){
            btnShowVersionHistory(assetId);
        }
    };

    register(shareButton, 0);
    register(downloadButton, 1);
    register(renameButton, 2);
    register(copyButton, 3);
    register(moveButton, 4);
    register(subscribeButton, 5);
    register(deleteButton, 6);
    register(showVersionHistoryButton, 7);
});