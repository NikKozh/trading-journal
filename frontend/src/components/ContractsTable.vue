<template>
    <el-table :data="contracts" border style="width: 100%">
        <el-table-column prop="number" label="№"></el-table-column>
        <el-table-column prop="contractType" label="Счёт"></el-table-column>
        <el-table-column prop="created" label="Дата"></el-table-column>
        <el-table-column prop="expiration" label="Экспирация"></el-table-column>
        <el-table-column prop="fxSymbol" label="Актив"></el-table-column>
        <el-table-column prop="direction" label="Прогноз"></el-table-column>
        <el-table-column prop="buyPrice" label="Вложение"></el-table-column>
        <el-table-column prop="profitPercent" label="%"></el-table-column>
        <el-table-column prop="isWin" label="Результат"></el-table-column>
        <el-table-column prop="income" label="Доход"></el-table-column>
        <el-table-column prop="tags" label="Тэги"></el-table-column>
        <el-table-column prop="isCorrect" label="По ТС?"></el-table-column>
        <el-table-column prop="" label="Действия"></el-table-column>
    </el-table>
</template>

<script lang="ts">
    import Vue from "vue"
    import {Component} from "vue-property-decorator";
    import {fetchAndResolve, fetchAndResolveArray} from "../utils/apiJsonResolver";
    import ApiRoutes from "../router/ApiRoutes";
    import DetailedError from "../models/DetailedError";
    import Contract from "../models/Contract";

    @Component
    export default class ContractsTable extends Vue {
        contracts: Contract[] = []

        created() {
            fetchAndResolveArray(
                ApiRoutes.contractList,
                Contract,
                (contracts: Contract[]) => {
                    console.log("DONE: ", contracts)
                    this.contracts = contracts
                },
                (error: DetailedError) => {
                    console.log("ERROR: ", error)
                }
            )
        }
    }
</script>

<style scoped>

</style>