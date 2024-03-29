<template>
    <el-form :model="contract" ref="contractRef" :rules="validationRules" label-width="130px" label-position="left">
        <el-col :span="12">
            <el-form-item v-if="isContractNew()" label="Номер сделки">
                <el-input-number v-model="contract.number"></el-input-number>
            </el-form-item>
            <el-form-item label="Дата сделки" prop="created">
                <el-date-picker v-model="contract.created" type="date" format="dd.MM.yyyy" value-format="timestamp">
                </el-date-picker>
            </el-form-item>
            <el-form-item label="Тип счёта" prop="contractType">
                <el-select v-model="contract.contractType" placeholder="Тип счёта">
                    <!-- TODO: server-side options -->
                    <el-option label="Демо" value="Демо"></el-option>
                    <el-option label="Реал" value="Реал"></el-option>
                    <el-option label="Форкаст" value="Форкаст"></el-option>
                </el-select>
            </el-form-item>
            <el-form-item label="Актив" prop="fxSymbol">
                <!-- TODO: server-side options -->
                <el-select v-model="contract.fxSymbol" placeholder="Актив">
                    <el-option label="EUR/USD" value="EUR/USD"></el-option>
                    <el-option label="USD/JPY" value="USD/JPY"></el-option>
                </el-select>
            </el-form-item>
            <el-form-item label="Прогноз" prop="direction">
                <!-- TODO: server-side options -->
                <el-select v-model="contract.direction" placeholder="Прогноз">
                    <el-option label="CALL" value="CALL"></el-option>
                    <el-option label="PUT" value="PUT"></el-option>
                </el-select>
            </el-form-item>
            <el-form-item label="Экспирация, мин." prop="expiration">
                <el-input-number :min="1" v-model="contract.expiration"></el-input-number>
            </el-form-item>
        </el-col>
        <el-col :span="12">
            <el-form-item label="Вложение, $" label-width="100px" prop="buyPrice">
                <el-input-number :value="buyPrice" @input="handleBuyPriceInput" :precision="2" :step="0.1" :min="1">
                </el-input-number> <!-- TODO: маска -->
            </el-form-item>
            <el-form-item label="Прибыль, %" label-width="100px" prop="profitPercent">
                <el-input-number :value="profitPercent"
                                 @input="handleProfitPercentInput"
                                 :precision="2"
                                 :step="0.1"
                                 :min="1"
                ></el-input-number> <!-- TODO: маска -->
            </el-form-item>
            <el-form-item label="В плюс?" label-width="100px">
                <el-checkbox v-model="contract.isWin"></el-checkbox> <!-- TODO: радиобатоны вместо чекбокса? -->
            </el-form-item>
            <el-form-item label="Стратегия" label-width="100px">
                <el-input></el-input>
            </el-form-item>
            <el-form-item label="Вход по ТС?" label-width="100px">
                <el-checkbox v-model="contract.isCorrect"></el-checkbox> <!-- TODO: здесь вроде норм оставить чекбокс? -->
            </el-form-item>
        </el-col>
        <el-col>
            <el-form-item v-if="isContractNew()" label="Скриншоты">
                <el-input :value="screenshotsPath" @input="handleScreenshotsPathInput"></el-input>
            </el-form-item>
            <el-form-item label="Тэги">
                <el-input v-model="contract.tags"></el-input>
            </el-form-item>
            <el-form-item label="Описание">
                <el-input v-model="contract.description" type="textarea" :rows="18"></el-input>
            </el-form-item>
            <el-form-item label-width="0">
                <el-button type="primary" @click="submitForm">Сохранить</el-button>
                <!-- TODO: сейчас отмена не сбрасывает изменившиеся значения полей, надо сбрасывать -->
                <el-button @click="handleCancel">Отмена</el-button>
            </el-form-item>
        </el-col>
    </el-form>
</template>

<script lang="ts">
    import {Component, Prop} from "vue-property-decorator"
    import Vue from "vue"
    import Contract from "../models/Contract"
    import * as O from "fp-ts/es6/Option"
    import {smartJsonStringify} from "../utils/Helper"
    import ApiRoutes from "../router/ApiRoutes"
    import Routes from "../router/Routes"
    import {defaultActionOnError, submitWithRecovery} from "../utils/apiJsonResolver"
    import {Form} from "element-ui"
    import goToPage from "../router/goToPage"

    @Component
    export default class ContractForm extends Vue {
        @Prop()
        contract!: Contract

        $refs!: {
            contractRef: Form
        }

        isContractNew(): boolean {
            return this.$router.currentRoute.path === Routes.createContract
        }

        // TODO: обобщить повторяющиеся валидаторы
        validationRules = {
            created: [
                { type: 'date', required: true, message: 'Это поле обязательно для заполнения', trigger: 'change' }
            ],
            contractType: [
                { required: true, message: 'Это поле обязательно для заполнения', trigger: 'change' }
            ],
            fxSymbol: [
                { required: true, message: 'Это поле обязательно для заполнения', trigger: 'change' }
            ],
            direction: [
                { required: true, message: 'Это поле обязательно для заполнения', trigger: 'change' }
            ],
            expiration: [
                { required: true, message: 'Это поле обязательно для заполнения', trigger: 'blur' }
            ]
        }

        buyPrice: number = 0
        profitPercent: number = 0
        screenshotsPath: string = ""

        created() {
            this.buyPrice = this.contract ? Number.parseFloat(this.contract.buyPriceF(true)) : 0
            this.profitPercent = this.contract ? Number.parseFloat(this.contract.profitPercentF(true)) : 0
        }

        handleBuyPriceInput(value: number): void {
            this.contract.buyPrice = O.some(value)
            this.buyPrice = value
        }

        handleProfitPercentInput(value: number): void {
            this.contract.profitPercent = O.some(value / 100)
            this.profitPercent = value
        }

        handleScreenshotsPathInput(value: string): void {
            this.contract.screenshotPaths = value.split(";")
            this.screenshotsPath = value
        }

        private prepareContractSubmit() {
            if (O.exists(n => n === 0 || n === undefined)(this.contract.buyPrice)) {
                this.contract.buyPrice = O.none
            }
            if (O.exists((n: number) => n === 0 || n === undefined || Number.isNaN(n))(this.contract.profitPercent)) {
                this.contract.profitPercent = O.none
            }
        }

        submitForm() {
            (this.$refs.contractRef as Form).validate((valid: boolean) => {
                if (valid) {
                    this.prepareContractSubmit()

                    submitWithRecovery(
                        ApiRoutes.submitContract,
                        () => goToPage.contractCard(this.contract.id),
                        defaultActionOnError(),
                        {
                            method: "POST",
                            headers: {
                                'Content-Type': 'application/json'
                            },
                            body: smartJsonStringify(this.contract)
                        }
                    )
                } else {
                    return false
                }
            })
        }

        handleCancel() {
            goToPage.contractCard(this.contract.id)
        }
    }
</script>

<style>

</style>