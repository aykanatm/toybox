<template>
    <div class="ui modal" id="toybox-rename-modal-window">
        <i class="close icon"></i>
        <div class="header">Rename <span v-show="isAsset">File</span><span v-show="!isAsset">Folder</span></div>
        <div class="content" style="height: 100%;">
            <div style="margin-bottom: 10px;">
                <span>Please enter the new name for the <span v-show="isAsset">file</span><span v-show="!isAsset">folder</span></span>
            </div>
            <div class="ui input" style="width: 100%;">
                <input type="text" name="file-name" placeholder="Name" id="toybox-file-name" v-on:keyup.enter="renameItem" v-model="name">
            </div>
        </div>
        <div class="actions">
            <button class="ui primary button" v-on:click="renameItem">Submit</button>
        </div>
    </div>
</template>

<script src="../../components/asset-rename-modal-window/asset-rename-modal-window.js"></script>
<style src="../../components/asset-rename-modal-window/asset-rename-modal-window.css"></style>