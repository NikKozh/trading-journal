<template>

</template>

<script lang="ts">
    import Vue from "vue"
    import {Component} from "vue-property-decorator"
    import {removeAuthCookie} from "../utils/Helper"
    import goToPage from "../router/goToPage"
    import EventBus from "../utils/EventBus"

    @Component()
    export default class SignOut extends Vue {
        created() {
            removeAuthCookie()
            EventBus.$emit("show-demo-warning")
            goToPage.contractList()
        }
    }
</script>

<style scoped>

</style>