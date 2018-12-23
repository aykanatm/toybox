<template>
    <div v-bind:id="id" v-bind:class="{'ui':true, 'icon':true, 'negative':isError, 'positive':isOk, 'info':isInfo, 'warning':isWarning, 'message':true}">
        <i v-if="isInfo" class="info circle icon"></i>
        <i v-else-if="isOk" class="check circle icon"></i>
        <i v-else class="exclamation triangle icon"></i>
        <i class="close icon"></i>
        <div class="content">
            <div class="header">
                {{ header }}
            </div>
            <p>{{ message }}</p>
        </div>
    </div>
</template>

<script src="../../components/message/message.js"></script>
<style src="../../components/message/message.css"></style>