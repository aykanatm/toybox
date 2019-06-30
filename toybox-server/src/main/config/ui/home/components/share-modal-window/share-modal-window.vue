<template>
    <div class="ui modal" id="toybox-share-modal-window">
        <i class="close icon"></i>
        <div class="header">Share</div>
        <div class="content" style="height: 100%;">
            <div class="user-selection">
                <label style="margin-right: 10px;">Internal User</label>
                <div class="ui toggle checkbox" id="user-selection-toggle">
                    <input type="checkbox" name="usertype" v-model="isExternalUser">
                    <label>External User</label>
                </div>
            </div>
            <div class="share-settings">
                <div class="notification-settings">
                    <div>
                        <div class="ui toggle checkbox" id="toybox-toggle">
                            <input type="checkbox" name="notifyme" v-model="notifyMe">
                            <label>Notify</label>
                        </div>
                    </div>
                    <div class="notification-types">
                        <div>
                            <div class="ui toggle checkbox" id="toybox-toggle">
                                <input type="checkbox" name="notifymeonview" v-model="notifyOnView" v-bind:disabled="isExternalUser || !notifyMe">
                                <label>View</label>
                            </div>
                        </div>
                        <div>
                            <div class="ui toggle checkbox" id="toybox-toggle">
                                <input type="checkbox" name="notifymeonedit" v-model="notifyOnEdit" v-bind:disabled="isExternalUser || !notifyMe">
                                <label>Edit</label>
                            </div>
                        </div>
                        <div>
                            <div class="ui toggle checkbox" id="toybox-toggle">
                                <input type="checkbox" name="notifymeondownload" v-model="notifyOnDownload" v-bind:disabled="!notifyMe">
                                <label>Download</label>
                            </div>
                        </div>
                        <div>
                            <div class="ui toggle checkbox" id="toybox-toggle">
                                <input type="checkbox" name="notifymeonshare" v-model="notifyOnShare" v-bind:disabled="isExternalUser || !notifyMe">
                                <label>Share</label>
                            </div>
                        </div>
                        <div>
                            <div class="ui toggle checkbox" id="toybox-toggle">
                                <input type="checkbox" name="notifymeoncopyormove" v-model="notifyOnMoveOrCopy" v-bind:disabled="isExternalUser || !notifyMe">
                                <label>Copy or Move</label>
                            </div>
                        </div>
                    </div>
                </div>
                <div class="permission-settings">
                    <div style="font-weight: bold;">
                        <span>Permissions</span>
                    </div>
                    <div class="permission-types">
                        <div>
                            <div class="ui toggle checkbox" id="toybox-toggle">
                                <input type="checkbox" name="notifymeonview" v-model="canView" v-bind:disabled="isExternalUser">
                                <label>View</label>
                            </div>
                        </div>
                        <div>
                            <div class="ui toggle checkbox" id="toybox-toggle">
                                <input type="checkbox" name="notifymeonedit" v-model="canEdit" v-bind:disabled="isExternalUser">
                                <label>Edit</label>
                            </div>
                        </div>
                        <div>
                            <div class="ui toggle checkbox" id="toybox-toggle">
                                <input type="checkbox" name="notifymeondownload" v-model="canDownload" v-bind:disabled="isExternalUser">
                                <label>Download</label>
                            </div>
                        </div>
                        <div>
                            <div class="ui toggle checkbox" id="toybox-toggle">
                                <input type="checkbox" name="notifymeonshare" v-model="canShare" v-bind:disabled="isExternalUser">
                                <label>Share</label>
                            </div>
                        </div>
                        <div>
                            <div class="ui toggle checkbox" id="toybox-toggle">
                                <input type="checkbox" name="notifymeoncopyormove" v-model="canMoveOrCopy" v-bind:disabled="isExternalUser">
                                <label>Copy or Move</label>
                            </div>
                        </div>
                    </div>
                </div>
                <div class="internal-user-share" v-show="!isExternalUser">
                    <div style="margin-bottom: 10px; margin-left: 5px; font-weight: bold;">
                        <span>Select users or usergroups to share the content with:</span>
                    </div>
                    <div>
                        <select class="ui fluid search dropdown" multiple="" id="user-dropdown">
                            <option value="">User or Usergroup</option>
                            <option v-for="user in users" v-bind:value="user.username" v-bind:key="user.user_id">{{user.username}}</option>
                        </select>
                    </div>
                </div>
                <div class="external-user-share" v-show="isExternalUser">
                    <div style="width: 70%;">
                        <div class="ui input" style="width: 80%;">
                            <input type="text" placeholder="Share URL" v-model="externalShareUrl">
                        </div>
                        <button class="ui primary button" style="width: 19%;" v-on:click.stop="copy">
                            Copy
                        </button>
                    </div>
                </div>
            </div>
        </div>
        <div class="actions">
            <div v-show="isExternalUser" class="ui primary button" v-on:click="generateUrl">Generate URL</div>
            <div v-show="!isExternalUser" class="ui primary button" v-on:click="share">Share</div>
        </div>
    </div>
</template>

<script src="../../components/share-modal-window/share-modal-window.js"></script>
<style src="../../components/share-modal-window/share-modal-window.css"></style>
