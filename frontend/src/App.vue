<template>
    <div id="app">
        <el-container>
            <el-header>
                <Header></Header>
            </el-header>
            <el-main>
                <router-view></router-view>
            </el-main>
        </el-container>
    </div>
</template>

<script lang="ts">
    import Vue from "vue"
    import {Component} from "vue-property-decorator"
    import Header from "./components/Header.vue"

    @Component({
        components: { Header }
    })
    export default class App extends Vue { }
</script>

<style scoped>
</style>