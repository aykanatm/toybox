<template>
    <div class="ui modal" id="toybox-share-modal-window">
        <i class="close icon"></i>
        <div class="header">Share</div>
        <div class="content" style="height: 100%; position: relative;">
            <div class="ui active inverted dimmer" style="position: absolute;" v-if="isSharing">
                <div class="ui massive text loader">Generating the share...</div>
            </div>
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
                                <input type="checkbox" name="notifymeoncopy" v-model="notifyOnMoveOrCopy" v-bind:disabled="isExternalUser || !notifyMe">
                                <label>Copy</label>
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
                                <input type="checkbox" name="canedit" v-model="canEdit" v-bind:disabled="isExternalUser">
                                <label>Edit</label>
                            </div>
                        </div>
                        <div>
                            <div class="ui toggle checkbox" id="toybox-toggle">
                                <input type="checkbox" name="candownload" v-model="canDownload" v-bind:disabled="isExternalUser">
                                <label>Download</label>
                            </div>
                        </div>
                        <div>
                            <div class="ui toggle checkbox" id="toybox-toggle">
                                <input type="checkbox" name="canshare" v-model="canShare" v-bind:disabled="isExternalUser">
                                <label>Share</label>
                            </div>
                        </div>
                        <div>
                            <div class="ui toggle checkbox" id="toybox-toggle">
                                <input type="checkbox" name="cancopy" v-model="canCopy" v-bind:disabled="isExternalUser">
                                <label>Copy</label>
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
                            <option v-for="userOrUsergroup in usersAndUsergroups" v-bind:value="userOrUsergroup.displayName" v-bind:key="userOrUsergroup.id">{{userOrUsergroup.displayName}}</option>
                        </select>
                    </div>
                    <div style="margin-bottom: 5px; margin-left: 5px; font-weight: bold; margin-top: 5px;">
                        <span>Expiration</span>
                    </div>
                    <div style="padding-left: 20px;">
                        <div>
                            <div class="ui toggle checkbox" id="toybox-toggle">
                                <input type="checkbox" name="canexpireinternal" v-model="enableExpireInternal">
                                <label>Enable Expiration</label>
                            </div>
                        </div>
                        <div class="ui calendar" id="internal-expiration-date" style="display: inline-block; margin-top: 5px;">
                            <div class="ui input right icon">
                                <i class="calendar icon"></i>
                                <input type="text" id="internal-expiration-date-input" placeholder="Expiration Date">
                            </div>
                        </div>
                    </div>
                </div>
                <div class="external-user-share" v-show="isExternalUser">
                    <div style="margin-bottom: 5px; margin-left: 5px; font-weight: bold; margin-top: 5px;">
                        <span>Usage Limit</span>
                    </div>
                    <div style="padding-left: 20px;">
                        <div>
                            <div class="ui toggle checkbox" id="toybox-toggle">
                                <input type="checkbox" name="enableusagelimit" v-model="enableUsageLimit">
                                <label>Enable Usage Limit</label>
                            </div>
                        </div>
                        <div style="padding-left: 10px;">
                            <div class="ui input">
                                <input id="max-number-of-hits" type="number" placeholder="Maximum Usage" v-model="maxNumberOfHits">
                            </div>
                        </div>
                    </div>
                    <div style="margin-bottom: 5px; margin-left: 5px; font-weight: bold; margin-top: 5px;">
                        <span>Expiration</span>
                    </div>
                    <div style="padding-left: 20px;">
                        <div>
                            <div class="ui toggle checkbox" id="toybox-toggle">
                                <input type="checkbox" name="canexpireexternal" v-model="enableExpireExternal">
                                <label>Expires</label>
                            </div>
                        </div>
                        <div class="ui calendar" id="external-expiration-date" style="display: inline-block; margin-top: 5px;">
                            <div class="ui input right icon">
                                <i class="calendar icon"></i>
                                <input type="text" id="external-expiration-date-input" placeholder="Expiration Date">
                            </div>
                        </div>
                    </div>
                    <div style="margin-top: 5px;">
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
            <div v-show="isExternalUser" v-bind:class="{'ui':true, 'primary':true, 'button':true, 'disabled':isSharing}" v-on:click="generateUrl">Generate URL</div>
            <div v-show="!isExternalUser" v-bind:class="{'ui':true, 'primary':true, 'button':true, 'disabled':isSharing}" v-on:click="share">Share</div>
        </div>
    </div>
</template>

<script src="../../components/share-modal-window/share-modal-window.js"></script>
<style src="../../components/share-modal-window/share-modal-window.css"></style>