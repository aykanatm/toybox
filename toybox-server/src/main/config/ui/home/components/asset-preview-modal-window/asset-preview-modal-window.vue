<template>
    <div class="ui modal" id="toybox-asset-preview-modal-window">
        <i class="share alternate icon asset-preview-modal-window-menu-icon" v-on:click="assetShare"></i>
        <i class="download icon asset-preview-modal-window-menu-icon" v-on:click="assetDownload"></i>
        <i class="i cursor icon asset-preview-modal-window-menu-icon" v-on:click="assetRename"></i>
        <i class="copy icon asset-preview-modal-window-menu-icon" v-on:click="assetCopy"></i>
        <i class="external alternate icon asset-preview-modal-window-menu-icon" v-on:click="assetMove"></i>
        <i class="rss icon asset-preview-modal-window-menu-icon" v-on:click="assetSubscribe"></i>
        <i class="list alternate outline icon asset-preview-modal-window-menu-icon" v-on:click="assetShowVersionHistory"></i>
        <i class="trash alternate outline icon asset-preview-modal-window-menu-icon" v-on:click="assetDelete"></i>

        <i class="close icon" style="right: 0px !important; color: white;"></i>
        <div class="content">
            <div style="height: 100%; width: 100%;">
                <div class="asset-preview-modal-window-asset-left-arrow" v-on:click="previousAsset">
                    <i v-bind:class="{'arrow':true, 'alternate': true, 'circle':true, 'left':true, 'icon':true, 'asset-preview-modal-window-asset-arrow-icon':true, 'disabled':!canNavigateToPreviousAsset}"></i>
                </div>
                <div v-if="hasPreview" style="height: 100%; width: 100%;">
                    <div v-if="isImage" class="asset-preview-modal-window-preview-container">
                        <img class="ui centered image" v-bind:src="previewUrl" v-on:error="onPreviewImgSrcNotFound"/>
                    </div>
                    <div v-else-if="isVideo" class="asset-preview-modal-window-preview-container">
                        <video v-bind:src="previewUrl" type="video/mp4" controls>
                    </div>
                    <div v-else-if="isAudio" class="asset-preview-modal-window-preview-container">
                        <audio v-bind:src="previewUrl" type="audio/mpeg" controls>
                    </div>
                    <div v-else-if="isDocument" class="asset-preview-modal-window-preview-container">
                        <iframe v-bind:src="previewUrl" style="width: 100%; height: 100%; border: none;"></iframe>
                    </div>
                    <div v-else class="asset-preview-modal-window-preview-container">
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
                <div class="asset-preview-modal-window-asset-right-arrow" v-on:click="nextAsset">
                    <i v-bind:class="{'arrow':true, 'alternate': true, 'circle':true, 'right':true, 'icon':true, 'asset-preview-modal-window-asset-arrow-icon':true, 'disabled':!canNavigateToNextAsset}"></i>
                </div>
            </div>
            <div class="asset-preview-modal-window-asset-name">
                <span>{{ assetName }}</span>
            </div>
        </div>
    </div>
</template>

<script src="../../components/asset-preview-modal-window/asset-preview-modal-window.js"></script>
<style src="../../components/asset-preview-modal-window/asset-preview-modal-window.css"></style>
