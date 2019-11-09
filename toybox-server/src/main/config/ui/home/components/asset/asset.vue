<template>
    <div v-bind:class="{'ui':true, 'fluid':true, 'card':true, 'toybox-card':true, 'toybox-card-selected':isSelected}" v-bind:id="id" v-on:click.stop="onClick" v-on:contextmenu="onRightClick($event)" v-on:mouseleave="onMouseLeave" v-on:dblclick="onDoubleClick">
        <div class="ui fluid image toybox-corner-label" v-show="isSelected">
            <span class="ui blue right corner label" style="overflow: hidden;"></span>
            <img/>
        </div>
        <div class="content" style="height: 50px;">
            <div style="float: left;">
                <img class="ui mini circular image" v-bind:src="userAvatarUrl" style="max-width: 28px;"/> {{ importedByUsername }}
            </div>
            <div v-show="shared === 'Y'" style="float: right; margin-top: 5px; font-size: 12pt;" v-bind:title="sharedByUsername">
                <i class="share alternate icon"></i>
            </div>
        </div>
        <div class="ui fluid image" style="z-index: 1; clear: both;">
            <div class="ui blue ribbon label" style="font-size: 1em;">
                <i v-if="isImage" class="file image icon"></i>
                <i v-else-if="isPdf" class="file pdf icon"></i>
                <i v-else-if="isWord" class="file word icon"></i>
                <i v-else-if="isPowerpoint" class="file powerpoint icon"></i>
                <i v-else-if="isExcel" class="file excel icon"></i>
                <i v-else-if="isAudio" class="file audio icon"></i>
                <i v-else-if="isVideo" class="file video icon"></i>
                <i v-else-if="isArchive" class="file archive icon"></i>
                <i v-else class="file icon"></i>
                 {{ extension }}
            </div>
            <img/>
        </div>
        <div class="toybox-card-img">
            <div v-if="hasThumbnail" style="height: 100%; display: flex; align-items: center; width: 100%;">
                <img class="ui centered image" v-bind:src="thumbnailUrl" style="max-width: 200px; max-height: 200px;" v-on:error="onThumbnailImgSrcNotFound"/>
            </div>
            <div v-else style="height: 100%; width: 100%; position: relative;">
                <i v-if="isImage" class="file image icon" style="position: absolute; font-size: 8em; top: 40%; left: 15%;"></i>
                <i v-else-if="isPdf" class="file pdf icon" style="position: absolute; font-size: 8em; top: 40%; left: 15%;"></i>
                <i v-else-if="isWord" class="file word icon" style="position: absolute; font-size: 8em; top: 40%; left: 15%;"></i>
                <i v-else-if="isPowerpoint" class="file powerpoint icon" style="position: absolute; font-size: 8em; top: 40%; left: 15%;"></i>
                <i v-else-if="isExcel" class="file excel icon" style="position: absolute; font-size: 8em; top: 40%; left: 15%;"></i>
                <i v-else-if="isAudio" class="file audio icon" style="position: absolute; font-size: 8em; top: 40%; left: 15%;"></i>
                <i v-else-if="isVideo" class="file video icon" style="position: absolute; font-size: 8em; top: 40%; left: 15%;"></i>
                <i v-else-if="isArchive" class="file archive icon" style="position: absolute; font-size: 8em; top: 40%; left: 15%;"></i>
                <i v-else class="file icon" style="position: absolute; font-size: 8em; top: 40%; left: 15%;"></i>
            </div>
        </div>
        <div class="content" style="overflow: hidden; height: 40px; text-overflow: ellipsis;">
            <span style="white-space: nowrap;">{{ name }}</span>
            <div v-show="contextMenuOpen" class="ui vertical menu toybox-asset-context-menu">
                <a class="item" v-on:click.stop="assetShare">
                    <i class="share alternate icon"></i>
                    Share
                </a>
                <a class="item" v-on:click.stop="assetDownload" v-show="canDownload === 'Y'">
                    <i class="download icon"></i>
                    Download
                </a>
                <a class="item" v-on:click.stop="assetRename" v-show="canEdit === 'Y'">
                    <i class="i cursor icon"></i>
                    Rename
                </a>
                <a class="item" v-on:click.stop="assetCopy" v-show="canCopy === 'Y'">
                    <i class="copy icon"></i>
                    Copy
                </a>
                <a class="item" v-on:click.stop="assetMove" v-show="!(shared === 'Y')">
                    <i class="external alternate icon"></i>
                    Move
                </a>
                <a class="item" v-on:click.stop="assetSubscribe" v-show="!(subscribed === 'Y')">
                    <i class="eye icon"></i>
                    Subscribe
                </a>
                <a class="item" v-on:click.stop="assetUnsubscribe" v-show="subscribed === 'Y'">
                    <i class="eye slash icon"></i>
                    Unsubscribe
                </a>
                <a class="item" v-on:click.stop="assetShowVersionHistory" v-show="canEdit === 'Y'">
                    <i class="list alternate outline icon"></i>
                    Show Version History
                </a>
                <a class="item" v-on:click.stop="assetDelete" v-show="!(shared === 'Y')">
                    <i class="trash alternate outline icon"></i>
                    Delete
                </a>
            </div>
        </div>
    </div>
</template>
<script src="../../components/asset/asset.js"></script>
<style src="../../components/asset/asset.css"></style>
