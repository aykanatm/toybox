<template>
    <div class="ui modal" id="toybox-asset-preview-modal-window">
        <div class="ui buttons" style="margin-left: 100px;">
            <div class="ui primary button">Actions</div>
            <div class="ui combo top right pointing dropdown primary icon button">
                <i class="dropdown icon"></i>
                <div class="menu">
                    <div class="item" v-on:click.stop="asset.assetShare"><i class="share alternate icon"></i> Share</div>
                    <div class="item" v-on:click.stop="asset.assetDownload"><i class="download icon"></i> Download</div>
                    <div class="item" v-on:click.stop="asset.assetRename"><i class="i cursor icon"></i> Rename</div>
                    <div class="item" v-on:click.stop="asset.assetCopy"><i class="copy icon"></i> Copy</div>
                    <div class="item" v-on:click.stop="asset.assetMove"><i class="external alternate icon"></i> Move</div>
                    <div class="item" v-on:click.stop="asset.assetSubscribe" v-show="!(asset.subscribed === 'Y')"><i class="eye icon"></i> Subscribe</div>
                    <div class="item" v-on:click.stop="asset.assetUnsubscribe" v-show="asset.subscribed === 'Y'"><i class="eye slash icon"></i> Unsubscribe</div>
                    <div class="item" v-on:click.stop="asset.assetShowVersionHistory"><i class="list alternate icon"></i> Show Version History</div>
                    <div class="item" v-on:click.stop="asset.assetDelete"><i class="trash alternate icon"></i> Delete</div>
                </div>
            </div>
        </div>
        <i class="close icon" style="right: 0px !important; color: white;"></i>
        <div class="content">
            <div style="height: 100%; width: 100%;">
                <div class="asset-preview-modal-window-asset-left-arrow" v-on:click="previousAsset">
                    <i v-bind:class="{'arrow':true, 'alternate': true, 'circle':true, 'left':true, 'icon':true, 'asset-preview-modal-window-asset-arrow-icon':true, 'disabled':!canNavigateToPreviousAsset}"></i>
                </div>
                <div v-if="hasPreview" style="height: 100%; width: 100%;">
                    <div v-if="asset.isImage" class="asset-preview-modal-window-preview-container">
                        <img class="ui centered image" v-bind:src="asset.previewUrl" style="max-height: 100%;"/>
                    </div>
                    <div v-else-if="asset.isVideo" class="asset-preview-modal-window-preview-container">
                        <video v-bind:src="asset.previewUrl" type="video/mp4" controls>
                    </div>
                    <div v-else-if="asset.isAudio" class="asset-preview-modal-window-preview-container">
                        <audio v-bind:src="asset.previewUrl" type="audio/mpeg" controls>
                    </div>
                    <div v-else-if="asset.isDocument" class="asset-preview-modal-window-preview-container">
                        <iframe v-bind:src="asset.previewUrl" style="width: 100%; height: 100%; border: none;"></iframe>
                    </div>
                    <div v-else class="asset-preview-modal-window-preview-container">
                        <img class="ui centered image" v-bind:src="asset.previewUrl"/>
                    </div>
                </div>
                <div v-else class="toybox-preview-window-icons">
                    <i v-if="asset.isImage" class="file image icon"></i>
                    <i v-else-if="asset.isPdf" class="file pdf icon"></i>
                    <i v-else-if="asset.isWord" class="file word icon"></i>
                    <i v-else-if="asset.isPowerpoint" class="file powerpoint icon"></i>
                    <i v-else-if="asset.isExcel" class="file excel icon"></i>
                    <i v-else-if="asset.isAudio" class="file audio icon"></i>
                    <i v-else-if="asset.isVideo" class="file video icon"></i>
                    <i v-else-if="asset.isArchive" class="file archive icon"></i>
                    <i v-else class="file icon"></i>
                </div>
                <div class="asset-preview-modal-window-asset-right-arrow" v-on:click="nextAsset">
                    <i v-bind:class="{'arrow':true, 'alternate': true, 'circle':true, 'right':true, 'icon':true, 'asset-preview-modal-window-asset-arrow-icon':true, 'disabled':!canNavigateToNextAsset}"></i>
                </div>
            </div>
            <div class="asset-preview-modal-window-asset-name">
                <span>{{ asset.name }}</span>
            </div>
        </div>
    </div>
</template>

<script src="../../components/asset-preview-modal-window/asset-preview-modal-window.js"></script>
<style src="../../components/asset-preview-modal-window/asset-preview-modal-window.css"></style>
