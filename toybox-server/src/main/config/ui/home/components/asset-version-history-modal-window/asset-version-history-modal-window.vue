<template>
    <div class="ui modal" id="toybox-asset-version-history-modal-window">
        <i class="close icon"></i>
        <div class="header">Version History</div>
        <div class="content" style="height: 100%;">
            <div class="toybox-version-history-table">
                <div class="ui grid toybox-version-history-header-grid">
                    <div class="eight column row toybox-version-history-header-row">
                        <div class="one wide column">
                            <span>Version</span>
                        </div>
                        <div class="two wide column">
                            <span>Import Date</span>
                        </div>
                        <div class="two wide column">
                            <span>Username</span>
                        </div>
                        <div class="two wide column">
                            <span>Thumbnail</span>
                        </div>
                        <div class="six wide column">
                            <span>Name</span>
                        </div>
                        <div class="one wide column">
                            <span>Size</span>
                        </div>
                        <div class="one wide column">

                        </div>
                    </div>
                </div>
                <div class="toybox-version-history-rows">
                    <div class="ui grid">
                        <asset-version-history v-for="versionHistoryAsset in versionHistoryAssets" v-bind:id="versionHistoryAsset.id"
                        v-bind:name="versionHistoryAsset.name" v-bind:username="versionHistoryAsset.importedByUsername" v-bind:date="versionHistoryAsset.importDate"
                        v-bind:is-latest-version="versionHistoryAsset.isLatestVersion" v-bind:version="versionHistoryAsset.version" v-bind:rendition-url="renditionUrl"
                         v-bind:type="versionHistoryAsset.type" v-bind:size="versionHistoryAsset.fileSize" v-bind:asset-url="assetUrl" v-bind:key="versionHistoryAsset.id"></asset-version-history>
                    </div>
                </div>
            </div>
        </div>
    </div>
</template>

<script src="../../components/asset-version-history-modal-window/asset-version-history-modal-window.js"></script>
<style src="../../components/asset-version-history-modal-window/asset-version-history-modal-window.css"></style>