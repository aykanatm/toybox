<template>
    <div class="event toybox-notification">
        <div class="label">
            <img class="ui avatar image" v-bind:src="avatarUrl"/>
        </div>
        <div class="content">
            <div class="summary">
                <div class="content">
                    <div class="date">
                        {{date}}
                    </div>
                    <div class="summary">
                        {{ message }}
                    </div>
                </div>
            </div>
        </div>
    </div>
</template>

<script src="../../components/notification/notification.js"></script>
<style src="../../components/notification/notification.css"></style>
