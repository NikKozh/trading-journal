<template>
    <el-menu :default-active="activeIndex"
             class="el-menu-demo"
             mode="horizontal"
             @select="handleSelect"
             menu-trigger="click"
    >
        <el-menu-item index="1">Главная</el-menu-item>

        <el-submenu index="2">
            <template slot="title">Создать сделку</template>

            <el-menu-item index="2-1">С нуля</el-menu-item>
            <el-menu-item index="2-2">Предзаполнить</el-menu-item>
        </el-submenu>

        <el-menu-item index="3">Статистика</el-menu-item>
    </el-menu>
</template>

<script lang="ts">
    import Vue from "vue"
    import {Component} from "vue-property-decorator";

    @Component
    export default class NavMenu extends Vue {
        activeIndex: string = "1"

        handleSelect(index: string, indexPath?: string) {
            console.log(index, indexPath)
        }
    }
</script>

<style scoped>

</style>