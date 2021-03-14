<template>
    <div id="header">
        <template v-if="!!needShowDemoWarning">
            <div id="demo-warning-line">
                Сайт работает в гостевом режиме. Все сделки являются демонстрационными.
                Допускается любое изменение данных.
            </div>
        </template>
        <NavMenu/>
    </div>
</template>

<script lang="ts">
    import Vue from "vue"
    import {Component} from "vue-property-decorator"
    import NavMenu from "./NavMenu.vue"
    import {isFullPermissions} from "../utils/Helper"
    import EventBus from "../utils/EventBus"

    @Component({
        components: { NavMenu }
    })
    export default class Header extends Vue {
        needShowDemoWarning: boolean | null = !isFullPermissions()

        created() {
            EventBus.$on("show-demo-warning", this.showDemoWarning)
            EventBus.$on("hide-demo-warning", this.hideDemoWarning)
        }

        showDemoWarning() {
            this.needShowDemoWarning = true
        }

        hideDemoWarning() {
            this.needShowDemoWarning = false
        }
    }
</script>

<style scoped>
    #demo-warning-line {
        width: 100%;
        background-color: #E6A23C;
        color: white;
        padding: 5px;
    }
</style>