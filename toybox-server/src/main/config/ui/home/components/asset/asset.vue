<template>
    <div v-bind:class="{'ui':true, 'fluid':true, 'card':true, 'toybox-card':true, 'toybox-card-selected':isSelected}" v-bind:id="assetId" v-on:click.stop="onClick">
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
                <button class="ui icon button" v-on:click.stop="share">
                    <i class="share alternate icon"></i>
                </button>
                <button class="ui icon button" v-on:click.stop="download">
                    <i class="download icon"></i>
                </button>
                <button class="ui icon button" v-on:click.stop="rename">
                    <i class="i cursor icon"></i>
                </button>
                <button class="ui icon button" v-on:click.stop="copy">
                    <i class="copy icon"></i>
                </button>
                <button class="ui icon button" v-on:click.stop="move">
                    <i class="external alternate icon"></i>
                </button>
                <button class="ui icon button" v-on:click.stop="subscribe">
                    <i class="rss icon"></i>
                </button>
                <button class="ui icon button" v-on:click.stop="showVersionHistory">
                    <i class="list alternate outline icon"></i>
                </button>
            </div>
        </div>
        <div class="extra content">
            <span>{{ assetName }}</span>
        </div>
    </div>
</template>
<script src="../../components/asset/asset.js"></script>
<style src="../../components/asset/asset.css"></style>
