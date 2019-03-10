<template>
    <div class="six column row toybox-job-row">
        <div class="column">{{ jobName }}</div>
        <div class="column">{{ jobType }}</div>
        <div class="column">{{ formattedStartTime }}</div>
        <div class="column">{{ formattedEndTime }}</div>
        <div class="column">{{ status }}</div>
        <div class="column">
            <button v-if="!isCompleted" class="mini ui button">Cancel</button>
            <button class="mini ui button" v-on:click="showJobDetailsModalWindow">View</button>
            <button v-if="isCompleted && isExport" class="mini ui button" v-on:click.stop="downloadJobResult">Download</button>
        </div>
    </div>
</template>
<script src="../../components/job/job.js"></script>
<style src="../../components/job/job.css"></style>