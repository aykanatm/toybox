<template>
    <div v-bind:class="{'ui':true, 'fluid':true, 'card':true, 'toybox-card':true, 'toybox-card-selected':isSelected}" v-bind:id="id" v-on:click.stop="onClick" v-on:contextmenu="onRightClick($event)" v-on:mouseleave="onMouseLeave" v-on:dblclick="onDoubleClick">
        <div class="ui fluid image toybox-corner-label" v-show="isSelected">
            <span class="ui blue right corner label" style="overflow: hidden;"></span>
            <img/>
        </div>
        <div class="content" style="height: 50px;">
            <div style="float: left;">
                <img class="ui mini circular image" v-bind:src="userAvatarUrl" style="max-width: 28px;"/> {{ createdByUsername }}
            </div>
            <div v-show="shared === 'Y'" style="float: right; margin-top: 5px; font-size: 12pt;" v-bind:title="sharedByUsername">
                <i class="share alternate icon"></i>
            </div>
        </div>
        <div class="toybox-card-img">
            <div style="height: 100%; width: 100%; position: relative;">
                <i class="folder icon" style="position: absolute; font-size: 8em; top: 40%; left: 15%;"></i>
            </div>
        </div>
        <div class="content" style="overflow: hidden; height: 40px; text-overflow: ellipsis;">
            <span style="white-space: nowrap;">{{ name }}</span>
            <div v-show="contextMenuOpen" class="ui vertical menu toybox-folder-context-menu">
                <a class="item" v-on:click.stop="folderShare">
                    <i class="share alternate icon"></i>
                    Share
                </a>
                <a class="item" v-on:click.stop="folderDownload" v-show="canDownload === 'Y'">
                    <i class="download icon"></i>
                    Download
                </a>
                <a class="item" v-on:click.stop="folderRename">
                    <i class="i cursor icon"></i>
                    Rename
                </a>
                <a class="item" v-on:click.stop="folderCopy" v-show="canCopy === 'Y'">
                    <i class="copy icon"></i>
                    Copy
                </a>
                <a class="item" v-on:click.stop="folderMove" v-show="!(shared === 'Y')">
                    <i class="external alternate icon"></i>
                    Move
                </a>
                <a class="item" v-on:click.stop="folderSubscribe" v-show="!(subscribed === 'Y')">
                    <i class="eye icon"></i>
                    Subscribe
                </a>
                <a class="item" v-on:click.stop="folderUnsubscribe" v-show="subscribed === 'Y'">
                    <i class="eye slash icon"></i>
                    Unsubscribe
                </a>
                <a class="item" v-on:click.stop="folderDelete" v-show="!(shared === 'Y')">
                    <i class="trash alternate outline icon"></i>
                    Delete
                </a>
            </div>
        </div>
    </div>
</template>
<script src="../../components/folder/folder.js"></script>
<style src="../../components/folder/folder.css"></style>