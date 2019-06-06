<template>
    <div v-if="isSection" v-bind:class="{'active':isActive, 'section': true, 'navigatable': !isActive}" v-on:click.stop="onClick">
        {{ containerName }}
    </div>
    <div v-else class="divider"> > </div>
</template>

<script src="../../components/breadcrumb/breadcrumb.js"></script>
<style src="../../components/breadcrumb/breadcrumb.css"></style>
