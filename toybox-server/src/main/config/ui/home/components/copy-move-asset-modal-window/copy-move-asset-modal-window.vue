<template>
    <div class="ui modal" id="toybox-copy-move-asset-modal-window">
        <i class="close icon"></i>
        <div v-show="isMove" class="header">Move</div>
        <div v-show="isCopy" class="header">Copy</div>
        <div class="content" style="height: 100%;">
            <div id="folder-tree" style="height: 100%; overflow-y: auto;">

            </div>
        </div>
        <div class="actions">
            <div v-show="isCopy" class="ui primary button" v-on:click="copyAsset">Copy</div>
            <div v-show="isMove" class="ui primary button" v-on:click="moveAsset">Move</div>
        </div>
    </div>
</template>

<script src="../../components/copy-move-asset-modal-window/copy-move-asset-modal-window.js"></script>
<style src="../../components/copy-move-asset-modal-window/copy-move-asset-modal-window.css"></style>