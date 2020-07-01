<template>
    <div id="app">
        <ErrorAlert ref="errorAlert"
                    :error="occurredError"
                    v-on:close-alert="closeErrorAlert()"
        ></ErrorAlert>
        <el-container id="main-container">
            <el-header>
                <Header></Header>
            </el-header>
            <el-main id="main-element">
                <router-view></router-view>
            </el-main>
        </el-container>
    </div>
</template>

<script lang="ts">
    import Vue from "vue"
    import {Component} from "vue-property-decorator"
    import Header from "./components/Header.vue"
    import ErrorAlert from "./components/ErrorAlert.vue"
    import EventBus from "./utils/EventBus"
    import DetailedError from "./models/DetailedError"

    @Component({
        components: { Header, ErrorAlert }
    })
    export default class App extends Vue {
        // reactive TODO: написать свой декоратор?
        occurredError: DetailedError | null = null

        created() {
            EventBus.$on("error-occurred", (error: DetailedError) => this.openErrorAlert(error))
        }

        openErrorAlert(error: DetailedError) {
            // TODO: предусмотреть появление нескольких ошибок одновременно, сделав массив и переделав под это алерт
            this.occurredError = error
        }

        closeErrorAlert() {
            this.occurredError = null
        }
    }
</script>

<style>
    html, body, #app, #main-container, #main-view, #contracts-table {
        height: 100%;
    }
    body {
        margin: 0;
    }
    h1 {
        margin: 0;
        padding: 20px 0;
    }
</style>