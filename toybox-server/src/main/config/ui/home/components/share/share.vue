<template>
    <div class="fourteen column row share-row">
        <div class="column">
            <div v-if="type === 'com.github.murataykanat.toybox.dbo.InternalShare'" class="column">
                <span>Internal</span>
            </div>
            <div v-else class="column">
                <span>External</span>
            </div>
        </div>
        <div class="column">
            <div v-if="enableExpire === 'Y'" class="column">
                <i class="check green icon"></i>
            </div>
            <div v-else class="column">
                <i class="times red icon"></i>
            </div>
        </div>
        <div class="column">{{ formattedExpirationDate }}</div>
        <div class="column">
            <div v-if="enableUsageLimit === 'Y'" class="column">
                <i class="check green icon"></i>
            </div>
            <div v-else class="column">
                <i class="times red icon"></i>
            </div>
        </div>
        <div class="column">
            <div v-if="maxNumberOfHits" class="column">
                <span>{{ maxNumberOfHits }}</span>
            </div>
            <div v-else class="column">
                <span>N/A</span>
            </div>
        </div>
        <div class="column">
            <div v-if="canCopy === 'Y'" class="column">
                <i class="check green icon"></i>
            </div>
            <div v-else class="column">
                <i class="times red icon"></i>
            </div>
        </div>
        <div class="column">
            <div v-if="canDownload === 'Y'" class="column">
                <i class="check green icon"></i>
            </div>
            <div v-else class="column">
                <i class="times red icon"></i>
            </div>
        </div>
        <div class="column">
            <div v-if="canEdit === 'Y'" class="column">
                <i class="check green icon"></i>
            </div>
            <div v-else class="column">
                <i class="times red icon"></i>
            </div>
        </div>
        <div class="column">
            <div v-if="canShare === 'Y'" class="column">
                <i class="check green icon"></i>
            </div>
            <div v-else class="column">
                <i class="times red icon"></i>
            </div>
        </div>
        <div class="column">
            <div v-if="notifyOnCopy === 'Y'" class="column">
                <i class="check green icon"></i>
            </div>
            <div v-else class="column">
                <i class="times red icon"></i>
            </div>
        </div>
        <div class="column">
            <div v-if="notifyOnDownload === 'Y' || notifyWhenDownloaded === 'Y'" class="column">
                <i class="check green icon"></i>
            </div>
            <div v-else class="column">
                <i class="times red icon"></i>
            </div>
        </div>
        <div class="column">
            <div v-if="notifyOnEdit === 'Y'" class="column">
                <i class="check green icon"></i>
            </div>
            <div v-else class="column">
                <i class="times red icon"></i>
            </div>
        </div>
        <div class="column">
            <div v-if="notifyOnShare === 'Y'" class="column">
                <i class="check green icon"></i>
            </div>
            <div v-else class="column">
                <i class="times red icon"></i>
            </div>
        </div>
        <div class="column">
            <button class="mini ui button" v-on:click.stop="editShare">Edit</button>
        </div>
    </div>
</template>
<script src="../../components/share/share.js"></script>
<style src="../../components/share/share.css"></style>