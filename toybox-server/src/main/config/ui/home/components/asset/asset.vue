<template>
    <div v-bind:class="{'ui':true, 'fluid':true, 'card':true, 'toybox-card':true, 'toybox-card-selected':isSelected}" v-bind:id="assetId" v-on:click="onClick">
        <div class="ui fluid image toybox-corner-label" v-show="isSelected">
            <span class="ui blue right corner label" style="overflow: hidden;"></span>
            <img/>
        </div>
        <div class="content">
            <img class="ui avatar image" v-bind:src="userAvatarUrl"/> {{ importedBy }}
        </div>
        <div class="ui fluid image" style="z-index: 1;">
            <div class="ui blue ribbon label">
                <i class="file image icon"></i> {{ extension }}
            </div>
            <img/>
        </div>
        <div class="toybox-card-img">
            <img class="ui centered image" v-bind:src="thumbnailUrl"/>
        </div>
        <div class="content">
            <div class="ui small basic icon buttons">
                <!-- Asset buttons -->
            </div>
        </div>
        <div class="extra content">
            <span>{{ assetName }}</span>
        </div>
    </div>
</template>
<script src="../../components/asset/asset.js"></script>
<style src="../../components/asset/asset.css"></style>
