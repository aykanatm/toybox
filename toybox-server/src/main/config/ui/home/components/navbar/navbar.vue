<template>
    <div class="ui icon menu" style="margin: 0; border-radius: 0; border: 0;">
        <div class="item">
            <img src="http://via.placeholder.com/250x50" class="toybox-brand-logo">
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

        <div class="ui icon right menu">
            <div class="ui category search item toybox-search-box">
                <div class="ui transparent icon input">
                    <input class="prompt" placeholder="Search..." type="text"/>
                    <i class="search link icon"></i>
                </div>
            </div>
            <a class="item toybox-navbar-item" id="toybox-advsearch-menu">
                <i class="search plus icon"></i>
            </a>
            <div class="ui simple dropdown item" id="toybox-profile-menu">
                <img class="ui avatar image" v-bind:src="user.avatarUrl"/>
                <div class="menu">
                    <a class="item">
                        <i class="edit icon"></i>
                        Edit Profile
                    </a>
                    <a class="item">
                        <i class="question circle icon"></i>
                        Help
                    </a>
                    <a class="item">
                        <i class="power off icon"></i>
                        Sign Off
                    </a>
                </div>
            </div>
        </div>
    </div>
</template>

<script src="../../components/navbar/navbar.js"></script>
<style src="../../components/navbar/navbar.css"></style>