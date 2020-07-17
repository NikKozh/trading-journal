<template>
    <!-- TODO: добавить куда-нибудь кнопки редактирования и удаления -->
    <div id="contract-view">
        <ContractInfoBox label="Тип счёта">{{ contract.contractType }}</ContractInfoBox>
        <ContractInfoBox label="Актив">{{ contract.fxSymbol }}</ContractInfoBox>
        <ContractInfoBox label="Прогноз">{{ contract.direction }}</ContractInfoBox>
        <ContractInfoBox label="Экспирация">{{ contract.expirationF() }}</ContractInfoBox>

        <el-divider class="contract-info-divider"></el-divider>

        <!-- TODO: добавить обработку опциональности (отсутствующее значение) -->
        <ContractInfoBox label="Вложение">{{ contract.buyPriceF() }}</ContractInfoBox>
        <ContractInfoBox label="Процент прибыли">{{ contract.profitPercentF() }}</ContractInfoBox>
        <ContractInfoBox label="Итог">{{ contract.isWinF() }}</ContractInfoBox>
        <ContractInfoBox label="Доход">{{ contract.incomeF() }}</ContractInfoBox>

        <el-divider class="contract-info-divider"></el-divider>

        <ContractInfoBox label="Тэги">{{ contract.tags }}</ContractInfoBox>
        <ContractInfoBox label="Вход по ТС?">{{ contract.isCorrectF() }}</ContractInfoBox>

        <el-divider class="contract-info-divider"></el-divider>

        <ContractInfoBox stacked label="Описание">
            <p v-for="line of contract.description.match(/[^\r\n]+/g)">
                {{ line }}
            </p>
        </ContractInfoBox>
    </div>
</template>

<script lang="ts">
    import {Component, Prop} from "vue-property-decorator"
    import Vue from "vue"
    import Contract from "../models/Contract"
    import ContractInfoBox from "./ContractInfoBox.vue"

    @Component({
        components: { ContractInfoBox }
    })
    export default class ContractView extends Vue {
        @Prop()
        contract!: Contract
    }
</script>

<style>
    .contract-info-divider {
        margin: 12px 0;
    }
</style>