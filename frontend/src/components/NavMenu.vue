<template>
    <el-menu :default-active="this.$route.path"
             class="el-menu-demo"
             mode="horizontal"
             menu-trigger="click"
             router
    >
        <el-menu-item :index="routes.index">Главная</el-menu-item>

        <el-submenu index="2">
            <template slot="title">Создать сделку</template>

            <el-menu-item :index="routes.createContract">С нуля</el-menu-item>
            <el-menu-item :index="routes.prefillContract">Предзаполнить</el-menu-item>
        </el-submenu>

        <el-menu-item :index="routes.stats">Статистика</el-menu-item>

        <template v-if="showSignIn()">
            <el-menu-item :index="routes.signIn">Войти</el-menu-item>
        </template>
        <template v-else>
            <el-menu-item :index="routes.signOut">Выйти</el-menu-item>
        </template>
    </el-menu>
</template>

<script lang="ts">
    import Vue from "vue"
    import {Component} from "vue-property-decorator"
    import Routes from "../router/Routes"
    import {isFullPermissions} from "../utils/Helper"

    @Component
    export default class NavMenu extends Vue {
        routes = Routes

        showSignIn(): boolean {
            return !isFullPermissions()
        }
    }
</script>

<style scoped>

</style>