<template>
    <div class="ui modal" id="toybox-asset-preview-modal-window">
        <i class="close icon" style="right: 0px !important; color: white;"></i>
        <div class="content">
            <div v-if="hasPreview">
                <div v-if="isImage">
                    <img class="ui centered image" v-bind:src="previewUrl" v-on:error="onPreviewImgSrcNotFound"/>
                </div>
                <div v-else-if="isVideo">
                    <video v-bind:src="previewUrl" type="video/mp4" controls>
                </div>
                <div v-else-if="isAudio">
                    <audio v-bind:src="previewUrl" type="audio/mpeg" controls>
                </div>
                <div v-else>
                    <img class="ui centered image" v-bind:src="previewUrl" v-on:error="onPreviewImgSrcNotFound"/>
                </div>
            </div>
            <div v-else class="toybox-preview-window-icons">
                <i v-if="isImage" class="file image icon"></i>
                <i v-else-if="isPdf" class="file pdf icon"></i>
                <i v-else-if="isWord" class="file word icon"></i>
                <i v-else-if="isPowerpoint" class="file powerpoint icon"></i>
                <i v-else-if="isExcel" class="file excel icon"></i>
                <i v-else-if="isAudio" class="file audio icon"></i>
                <i v-else-if="isVideo" class="file video icon"></i>
                <i v-else-if="isArchive" class="file archive icon"></i>
                <i v-else class="file icon"></i>
            </div>
            <div class="asset-preview-modal-window-asset-name">
                <span>{{ assetName }}</span>
            </div>
        </div>
    </div>
</template>

<script src="../../components/asset-preview-modal-window/asset-preview-modal-window.js"></script>
<style src="../../components/asset-preview-modal-window/asset-preview-modal-window.css"></style>
