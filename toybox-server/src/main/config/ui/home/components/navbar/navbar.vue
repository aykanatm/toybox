<template>
    <div class="ui icon menu" style="margin-bottom: 0">
        <div class="toybox-brand-logo">
        </div>
        <a class="item" id="toybox-home-menu" href="/toybox">
            <i class="home icon"></i>
        </a>
        <a class="item" id="toybox-files-menu" href="/toybox/files">
            <i class="file alternate icon"></i>
        </a>
        <a class="item" id="toybox-folders-menu" href="/toybox/folders">
            <i class="folder icon"></i>
        </a>

        <div class="ui icon right menu">
            <div class="ui category search item">
                <div class="ui transparent icon input">
                    <input class="prompt" placeholder="Search..." type="text"/>
                    <i class="search link icon"></i>
                </div>
            </div>
            <a class="item" id="toybox-advsearch-menu">
                <i class="search plus icon"></i>
            </a>
            <a class="item" id="toybox-profile-menu">
                <i class="user circle icon"></i>
            </a>
            <a class="item" id="toybox-help-menu">
                <i class="question circle icon"></i>
            </a>
            <a class="item" id="toybox-signoff-menu">
                <i class="power off icon"></i>
            </a>
        </div>
    </div>
</template>

<script src="../../components/navbar/navbar.js"></script>
<style src="../../components/navbar/navbar.css"></style>