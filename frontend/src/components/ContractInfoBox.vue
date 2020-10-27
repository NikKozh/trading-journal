<template>
    <div v-if="stacked" class="contract-info-row">
        <div class="contract-info-label">{{ label }}</div>
        <slot></slot>
    </div>
    <el-row v-else class="contract-info-row">
        <el-col :span="5" class="contract-info-label contract-info-label-inline">
            {{ label }}
        </el-col>
        <el-col :span="19">
            <slot></slot>
        </el-col>
    </el-row>
</template>

<script lang="ts">
    import {Component, Prop} from "vue-property-decorator"
    import Vue from "vue"

    @Component
    export default class ContractInfoBox extends Vue {
        @Prop()
        label!: string

        @Prop({ default: false })
        stacked!: boolean
    }
</script>

<style scoped>
    .contract-info-row {
        padding: 5px 0;
    }
    .contract-info-label {
        color: #707478;
        margin-bottom: 5px;
    }
    .contract-info-label-inline {
        color: #707478;
        margin-bottom: 0;
    }
</style>