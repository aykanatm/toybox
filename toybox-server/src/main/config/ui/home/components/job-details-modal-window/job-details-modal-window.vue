<template>
    <div class="ui modal" id="toybox-job-details-modal-window">
        <i class="close icon"></i>
        <div class="header">Job Details</div>
        <div class="content" style="height: 100%;">
            <div style="float: left; font-weight: bold;">
                <div>
                    <span>Job Name: </span>
                </div>
                <div>
                    <span>Job Type: </span>
                </div>
                <div>
                    <span>Start Time: </span>
                </div>
                <div>
                    <span>End Time: </span>
                </div>
                <div>
                    <span>Status: </span>
                </div>
                <div>
                    <span>Triggered by: </span>
                </div>
            </div>
            <div style="float: left; margin-left: 20px;">
                <div>
                    <span class="toybox-keep-spaces">{{ jobName || ' '}}</span>
                </div>
                <div>
                    <span class="toybox-keep-spaces">{{ jobType || ' '}}</span>
                </div>
                <div>
                    <span class="toybox-keep-spaces">{{ formattedStartTime || ' '}} </span>
                </div>
                <div>
                    <span class="toybox-keep-spaces">{{ formattedEndTime || ' '}}</span>
                </div>
                <div>
                    <span class="toybox-keep-spaces">{{ status || ' '}}</span>
                </div>
                <div>
                    <span class="toybox-keep-spaces">{{ username || ' '}}</span>
                </div>
            </div>
            <div style="clear: both; padding-top: 10px; height: calc(100% - 110px);">
                <div class="ui grid toybox-jobs-header-grid" style="padding-bottom: 14px;">
                    <div class="four column row toybox-jobs-header-row">
                        <div class="column">
                            <span>Step Name</span>
                        </div>
                        <div class="column">
                            <span>Start Time</span>
                        </div>
                        <div class="column">
                            <span>End Time</span>
                        </div>
                        <div class="column">
                            <span>Status</span>
                        </div>
                    </div>
                </div>
                <div style="height: calc(100% - 50px); overflow-y: auto; overflow-x: hidden">
                    <div class="ui grid" style="margin-left: 0px; margin-right: 0px;">
                        <job-step v-for="step in steps" v-bind:step-execution-id="step.stepExecutionId" v-bind:step-name="step.stepName"
                        v-bind:start-time="step.startTime" v-bind:end-time="step.endTime" v-bind:status="step.status" v-bind:key="step.stepExecutionId"/>
                    </div>
                </div>
            </div>
        </div>
    </div>
</template>

<script src="../../components/job-details-modal-window/job-details-modal-window.js"></script>
<style src="../../components/job-details-modal-window/job-details-modal-window.css"></style>