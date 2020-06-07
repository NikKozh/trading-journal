<template>
    <el-menu :default-active="this.$route.path"
             class="el-menu-demo"
             mode="horizontal"
             menu-trigger="click"
             :router="true"
    >
        <el-menu-item :index="routes.index">Главная</el-menu-item>

        <el-submenu index="2">
            <template slot="title">Создать сделку</template>

            <el-menu-item :index="routes.createContract">С нуля</el-menu-item>
            <el-menu-item index="2-2">Предзаполнить</el-menu-item>
        </el-submenu>

        <el-menu-item index="3">Статистика</el-menu-item>
    </el-menu>
</template>

<script lang="ts">
    import Vue from "vue"
    import {Component} from "vue-property-decorator"
    import Routes from "../router/Routes"

    @Component
    export default class NavMenu extends Vue {
        routes = Routes
    }
</script>

<style scoped>

</style>