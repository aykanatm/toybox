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
            <div v-if="type === 'com.github.murataykanat.toybox.dbo.ExternalShare' || canDownload === 'Y'" class="column">
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
            <div v-if="notifyOnDownload === 'Y'" class="column">
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
        <div class="column" style="padding-left: 0;">
            <div class="ui buttons">
                <div class="ui mini button">Actions</div>
                <div class="ui floating dropdown icon mini button share-action-dropdown">
                    <i class="dropdown icon"></i>
                    <div class="menu">
                        <div class="item" v-on:click.stop="editShare"><i class="pencil alternate icon"></i> Edit</div>
                        <div class="item" v-on:click.stop="deleteShare"><i class="trash alternate outline icon"></i> Delete</div>
                        <div class="item" v-on:click.stop="copyUrl" v-show="type === 'com.github.murataykanat.toybox.dbo.ExternalShare'"><i class="copy icon"></i> Copy URL</div>
                    </div>
                </div>
            </div>
        </div>
    </div>
</template>
<script src="../../components/share/share.js"></script>
<style src="../../components/share/share.css"></style>