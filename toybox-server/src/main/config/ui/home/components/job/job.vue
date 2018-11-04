<template>
    <div class="six column row toybox-job-row">
        <div class="column">{{ jobName }}</div>
        <div class="column">{{ jobType }}</div>
        <div class="column">{{ startTime }}</div>
        <div class="column">{{ endTime }}</div>
        <div class="column">{{ status }}</div>
        <div class="column">
            <button v-if="!isCompleted" class="ui button">Cancel</button>
            <button class="ui button">View</button>
            <button v-if="isCompleted && isExport" class="ui button">Download</button>
        </div>
    </div>
</template>
<script src="../../components/job/job.js"></script>
<style src="../../components/job/job.css"></style>