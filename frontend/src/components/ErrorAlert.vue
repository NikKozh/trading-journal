<template>
    <el-dialog :title="getTitle()"
               :visible.sync="visible"
               :before-close="handleClose"
               width="30%"
    >
        <div>
            <span v-html="getCauseBlock() + getDetailsBlock()" />
        </div>
        <span slot="footer" class="dialog-footer">
            <el-button class="el-button--primary" @click="handleClose">OK</el-button>
        </span>
    </el-dialog>
</template>

<script lang="ts">
    import Vue from "vue"
    import Component from "vue-class-component"
    import DetailedError from "../models/DetailedError"
    import {Prop} from "vue-property-decorator"
    import * as O from "fp-ts/es6/Option"
    import {pipe} from "fp-ts/es6/pipeable"

    @Component
    export default class ErrorAlert extends Vue {
        @Prop({ default: null })
        error!: DetailedError | null

        get visible(): boolean {
            return !!this.error
        }

        get errorOpt(): O.Option<DetailedError> {
            return O.fromNullable(this.error)
        }

        handleClose() {
            this.$emit('close-alert')
        }

        getTitle(): string {
            return pipe(this.errorOpt,
                O.map(err => err.caption),
                O.getOrElse(() => "")
            )
        }

        getCauseBlock(): string {
            return pipe(this.errorOpt,
                O.map((err: DetailedError) => `
                    <h3>Причина</h3>
                    <p>${err.cause}</p>
                `),
                O.getOrElse(() => "")
            )
        }

        getDetailsBlock(): string {
            return pipe(this.errorOpt,
                O.chain((err: DetailedError) => err.details),
                O.map((details: string) => `
                    <h3>Подробности</h3>
                    <p>${details}</p>
                `),
                O.getOrElse(() => "")
            )
        }
    }
</script>

<style scoped>
    /* TODO: стили */
</style>