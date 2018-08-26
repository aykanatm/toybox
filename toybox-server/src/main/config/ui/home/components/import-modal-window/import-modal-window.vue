<template>
    <div class="ui modal" id="toybox-import-modal-window">
        <i class="close icon"></i>
        <div class="header">Upload Files</div>
        <div class="content toybox-fit-parent toybox-upload-modal-content">
            <form class="toybox-fit-parent" enctype="multipart/form-data" novalidate>
                <div class="ui middle aligned grid toybox-fit-parent" style="margin-left:0px">
                    <input type="file" multiple style="height:  100%; width: 100%; z-index: 100; opacity:0" v-bind:name="uploadFieldName"
                    v-bind:disabled="isSaving" v-on:change="filesChange($event.target.name, $event.target.files);">
                    <div class="row" style="margin-top: -500px;">
                        <div class="column">
                            <div class="ui middle aligned ten column centered grid">
                                <i class="cloud upload icon toybox-upload-modal-icon"></i>
                            </div>
                        </div>
                    </div>
                    <div class="row" style="margin-top: -100px;">
                        <div class="column">
                            <div class="ui middle aligned four column centered grid">
                                <p class="toybox-upload-modal-text">Drag your files here or click to browse</p>
                            </div>
                        </div>
                    </div>
                </div>
            </form>
        </div>
    </div>
</template>

<script src="../../components/import-modal-window/import-modal-window.js"></script>
<style src="../../components/import-modal-window/import-modal-window.css"></style>
