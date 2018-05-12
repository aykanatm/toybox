$(document).ready(function () {
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
        secondaryClass: 'ui button',
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
        secondaryClass: 'ui button',
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
        secondaryClass: 'ui button',
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
        secondaryClass: 'ui button',
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
        secondaryClass: 'ui button',
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
        secondaryClass: 'ui button',
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
        secondaryClass: 'ui button',
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
        secondaryClass: 'ui button',
        text : 'Version History',
        icon : '<i class="list alternate outline icon"></i>',
        setup : function setup() {
            btnShowVersionHistorySetup();
        },
        click : function click(assetId){
            btnShowVersionHistory(assetId);
        }
    };

    toyboxui.galleryAssetView.register(shareButton, 0);
    toyboxui.galleryTopMenuView.register(shareButton, 0);

    toyboxui.galleryAssetView.register(downloadButton, 1);
    toyboxui.galleryTopMenuView.register(downloadButton, 1);

    toyboxui.galleryAssetView.register(renameButton, 2);
    toyboxui.galleryTopMenuView.register(renameButton, 2);

    toyboxui.galleryAssetView.register(copyButton, 3);
    toyboxui.galleryTopMenuView.register(copyButton, 3);

    toyboxui.galleryAssetView.register(moveButton, 4);
    toyboxui.galleryTopMenuView.register(moveButton, 4);

    toyboxui.galleryAssetView.register(subscribeButton, 5);
    toyboxui.galleryTopMenuView.register(subscribeButton, 5);

    toyboxui.galleryAssetView.register(deleteButton, 6);
    toyboxui.galleryTopMenuView.register(deleteButton, 6);

    toyboxui.galleryAssetView.register(showVersionHistoryButton, 7);
    toyboxui.galleryTopMenuView.register(showVersionHistoryButton, 7);
});