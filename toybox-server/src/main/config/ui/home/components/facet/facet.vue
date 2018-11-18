<template>
    <div>
        <div class="title active">
            <i class="dropdown icon"></i>
            {{ name }}
        </div>
        <div class="content">
            <facet-lookup v-for="lookup in lookups" v-bind:value="lookup" v-bind:key="lookup"/>
        </div>
    </div>
</template>
<script src="../../components/facet/facet.js"></script>
<style src="../../components/facet/facet.css"></style>
