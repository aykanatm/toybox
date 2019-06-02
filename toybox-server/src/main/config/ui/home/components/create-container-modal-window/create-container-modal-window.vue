<template>
    <div class="ui modal" id="toybox-create-container-modal-window">
        <i class="close icon"></i>
        <div class="header">New Folder</div>
        <div class="content" style="height: 100%;">
            <div style="margin-bottom: 10px;">
                <span>Please enter a name for the folder</span>
            </div>
            <div class="ui input" style="width: 100%;">
                <input type="text" name="folder-name" placeholder="Folder name" id="toybox-folder-name" v-on:keyup.enter="createContainer" v-model="containerName">
            </div>
            <div class="center" style="margin-top: 20px;">
                <button class="ui primary button" v-on:click="createContainer">Submit</button>
            </div>
        </div>
    </div>
</template>

<script src="../../components/create-container-modal-window/create-container-modal-window.js"></script>
<style src="../../components/create-container-modal-window/create-container-modal-window.css"></style>