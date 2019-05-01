<template>
    <div class="ui icon menu" style="margin: 0; border-radius: 0; border: 0;">
        <div class="item">
            <img src="../../images/logo.png" class="toybox-brand-logo">
        </div>
        <a class="item toybox-navbar-item" id="toybox-home-menu" href="/toybox">
            <i class="home icon"></i>
        </a>
        <a class="item toybox-navbar-item" id="toybox-files-menu" href="/toybox/files">
            <i class="file alternate icon"></i>
        </a>
        <a class="item toybox-navbar-item" id="toybox-folders-menu" href="/toybox/folders">
            <i class="folder icon"></i>
        </a>
        <a class="item toybox-navbar-item" id="toybox-folders-menu" href="/toybox/jobs">
            <i class="cogs icon"></i>
        </a>

        <div class="ui icon right menu">
            <div class="ui transparent icon input toybox-search-box">
                <input class="prompt" placeholder="Search..." type="text"/>
                <i class="search link icon"></i>
            </div>
            <a class="item toybox-navbar-item" id="toybox-advsearch-menu">
                <i class="search plus icon"></i>
            </a>
            <a class="item toybox-navbar-item" v-on:click="showUploadModalWindow">
                <i class="cloud upload icon"></i>
            </a>
            <div class="ui simple dropdown item toybox-navbar-item" id="toybox-notifications-menu">
                <i class="bell icon"></i>
                <div class="floating ui red circular mini label toybox-notification-label" v-show="notifications.length != 0">{{ notifications.length }}</div>
                <div class="left menu">
                    <div class="ui feed toybox-notification-feed" v-if="notifications.length == 0">
                        <notification v-bind:from-username="defaultNotification.fromUsername" v-bind:notification="defaultNotification.notification"
                        v-bind:notification-date="defaultNotification.notificationDate" v-bind:is-read="defaultNotification.isRead" v-bind:key="defaultNotification.id"/>
                    </div>
                    <div class="ui feed toybox-notification-feed" v-else>
                        <notification v-for="notification in notifications" v-bind:from-username="notification.fromUsername" v-bind:notification="notification.notification"
                        v-bind:notification-date="notification.notificationDate" v-bind:is-read="notification.isRead" v-bind:key="notification.id"/>
                    </div>
                </div>
            </div>
            <div class="ui simple dropdown item" id="toybox-profile-menu">
                <div v-if="userInitialized">
                    <img class="ui mini circular image" v-bind:src="user.avatarUrl"/>
                </div>
                <div v-else>
                    <i class="user circle icon" style="font-size: 30pt;"></i>
                </div>
                <div class="menu">
                    <a class="item">
                        <i class="edit icon"></i>
                        Edit Profile
                    </a>
                    <a class="item">
                        <i class="question circle icon"></i>
                        Help
                    </a>
                    <a class="item" v-on:click="logout">
                        <i class="power off icon"></i>
                        Sign Off
                    </a>
                </div>
            </div>
        </div>
        <import-modal-window/>
    </div>
</template>

<script src="../../components/navbar/navbar.js"></script>
<style src="../../components/navbar/navbar.css"></style>