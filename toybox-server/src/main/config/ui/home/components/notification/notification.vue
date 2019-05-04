<template>
    <div class="event toybox-notification">
        <div class="label">
            <div v-if="avatarLoadedSuccessfully">
                <img class="ui avatar image" v-bind:src="avatarUrl"/>
            </div>
            <div v-else>
                <i class="user circle icon" style="font-size: 20pt;"></i>
            </div>
        </div>
        <div class="content">
            <div class="summary">
                <div class="content">
                    <div class="date" v-show="!isDefaultNotification">
                        {{friendlyDate}}
                    </div>
                    <div class="summary">
                        {{ notification }}
                    </div>
                </div>
            </div>
            <div class="meta" v-show="!isDefaultNotification && isRead === 'N'">
                <a class="mark-as-unread" v-on:click.stop="markNotificationAsRead">
                    <i class="bell slash icon"></i> Mark as read
                </a>
            </div>
        </div>
    </div>
</template>

<script src="../../components/notification/notification.js"></script>
<style src="../../components/notification/notification.css"></style>
