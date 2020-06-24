<template>
    <div id="main-view">
        <ErrorAlert ref="errorAlert"
                    :error="fetchedError"
                    v-on:close-alert="closeErrorAlert()"
        ></ErrorAlert>
        <ContractsTable></ContractsTable>
    </div>
</template>

<script lang="ts">
    import Message from "../models/Message"
    import DetailedError from "../models/DetailedError"
    import Vue from "vue"
    import {Component} from "vue-property-decorator"
    import ErrorAlert from "../components/ErrorAlert.vue"
    import ContractsTable from "../components/ContractsTable.vue"

    @Component({
        components: { ErrorAlert, ContractsTable }
    })
    export default class Index extends Vue {
        signal: Message = new Message("Loading...", "N/A", 0)

        // reactive TODO: написать свой декоратор?
        fetchedError: DetailedError | null = null

        openErrorAlert(error: DetailedError) {
            this.fetchedError = error
        }

        closeErrorAlert() {
            this.fetchedError = null
        }
    }
</script>

<style scoped>
</style>