<template>
    <NavMenu/>
</template>

<script lang="ts">
    import Vue from "vue"
    import {Component} from "vue-property-decorator"
    import NavMenu from "./NavMenu.vue"

    @Component({
        components: { NavMenu }
    })
    export default class Header extends Vue {}
</script>

<style scoped>

</style>