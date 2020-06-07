<template>
    <div id="main-view">
        <ErrorAlert ref="errorAlert"
                    :error="fetchedError"
                    v-on:close-alert="closeErrorAlert()"
        ></ErrorAlert>

        <h2>Message: {{ signal.message }}</h2>
        <h2>Status: {{ signal.status }}</h2>
        <h2>Code: {{ signal.code }}</h2>
    </div>
</template>

<script lang="ts">
    import Message from "../models/Message"
    import {fetchAndResolve} from "../utils/apiJsonResolver"
    import ApiRoutes from "../router/ApiRoutes"
    import DetailedError from "../models/DetailedError"
    import Vue from "vue"
    import {Component} from "vue-property-decorator"
    import ErrorAlert from "../components/ErrorAlert.vue"

    @Component({
        components: { ErrorAlert }
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

        created() {
            fetchAndResolve(
                ApiRoutes.pingMessage,
                Message,
                (message: Message) => this.signal = message,
                (error: DetailedError) => {
                    this.openErrorAlert(error)
                    this.signal = new Message("error", "down", -1)
                }
            )
        }
    }
</script>

<style scoped>
</style>