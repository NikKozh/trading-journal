<template>
    <div id="main-view">
        <ContractsTable></ContractsTable>
    </div>
</template>

<script lang="ts">
    import Vue from "vue"
    import {Component} from "vue-property-decorator"
    import ContractsTable from "../components/ContractsTable.vue"

    @Component({
        components: { ContractsTable }
    })
    export default class Index extends Vue { }
</script>

<style scoped>
</style>