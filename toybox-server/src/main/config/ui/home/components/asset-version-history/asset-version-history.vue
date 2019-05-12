<template>
    <div class="eight column row toybox-asset-history-row" v-on:mouseenter="onMouseEnter" v-on:mouseleave="onMouseLeave">
        <div class="one wide column" style="text-align: center;">
            {{ version }}
        </div>
        <div class="two wide column">
            {{ importDate }}
        </div>
        <div class="two wide column">
            {{ username }}
        </div>
        <div class="two wide column" style="height: 100%;">
            <div v-if="hasThumbnail" style="height: 100%;">
                <img class="ui centered image" v-bind:src="thumbnailUrl" v-on:error="onThumbnailImgSrcNotFound" style="height: 100%;"/>
            </div>
            <div v-else class="toybox-asset-history-icon-container">
                <i v-if="isImage" class="file image icon toybox-asset-history-icon"></i>
                <i v-else-if="isPdf" class="file pdf icon toybox-asset-history-icon"></i>
                <i v-else-if="isWord" class="file word icon toybox-asset-history-icon"></i>
                <i v-else-if="isPowerpoint" class="file powerpoint icon toybox-asset-history-icon"></i>
                <i v-else-if="isExcel" class="file excel icon toybox-asset-history-icon"></i>
                <i v-else-if="isAudio" class="file audio icon toybox-asset-history-icon"></i>
                <i v-else-if="isVideo" class="file video icon toybox-asset-history-icon"></i>
                <i v-else-if="isArchive" class="file archive icon toybox-asset-history-icon"></i>
                <i v-else class="file icon"></i>
            </div>
        </div>
        <div class="six wide column">
            {{ name }}
        </div>
        <div class="one wide column">
            {{ size }}
        </div>
        <div class="one wide column">
            <button class="ui icon button" v-show="isHovered && isLatestVersion === 'N'" v-on:click="revert">
                <i class="history icon"></i>
            </button>
        </div>
    </div>
</template>
<script src="../../components/asset-version-history/asset-version-history.js"></script>
<style src="../../components/asset-version-history/asset-version-history.css"></style>