<template>
    <div id="contract-prefill">
        <el-page-header @back="handleBack" title="Назад">
            <h1 id="contract-header-text" slot="content">Предзаполнить сделку</h1>
        </el-page-header>
        <el-form :model="form" ref="form" :rules="rules" label-width="130px" label-position="top">
            <el-form-item label="Первая строчка: номер транзакции; вторая и последующие: ссылки на скриншоты"
                          prop="prefillData"
            >
                <el-input v-model="form.prefillData" type="textarea" :rows="18"></el-input>
            </el-form-item>
            <el-form-item label-width="0">
                <el-button type="primary" :loading="loading" @click="submitForm">Отправить</el-button>
                <el-button @click="handleBack">Отмена</el-button>
            </el-form-item>
        </el-form>
    </div>
</template>

<script lang="ts">
    import {Component} from "vue-property-decorator"
    import Vue from "vue"
    import {Form} from "element-ui"
    import * as A from "fp-ts/es6/Array"
    import * as O from "fp-ts/es6/Option"
    import {defaultActionOnError, simpleRequest} from "../utils/apiJsonResolver"
    import ApiRoutes from "../router/ApiRoutes"
    import Routes from "../router/Routes"
    import goToPage from "../router/goToPage";

    @Component
    export default class ContractInfoBox extends Vue {
        form = { prefillData: `
В качестве тестовых данных можно использовать:

1 вариант:
51984715861
https://i.ibb.co/mtd5B4N/Contract.png

2 вариант:
51982957741
https://i.ibb.co/mtd5B4N/Contract.png
        ` }

        rules = {
            prefillData: [
                { required: true, message: 'Это поле обязательно для заполнения', trigger: 'change' }
                // TODO: добавить валидацию на сам формат всей строки (транзакция + скриншоты)
            ]
        }

        $refs! = {
            form: Form
        }

        loading: boolean = false

        submitForm() {
            (this.$refs.form as Form).validate((valid: boolean) => {
                if (valid) {
                    this.loading = true

                    const lines = this.form.prefillData.split("\n")
                    const transactionId = lines[0]
                    const screenshotUrls = O.fold(
                        () => "",
                        (urls: string[]) => urls.join(";")
                    )(A.tail(lines))

                    simpleRequest(
                        ApiRoutes.prefillContract,
                        goToPage.contractForm,
                        defaultActionOnError(_ => this.loading = false),
                        {
                            method: "POST",
                            headers: {
                                'Content-Type': 'application/json'
                            },
                            body: JSON.stringify({
                                transactionId: transactionId,
                                screenshotUrls: screenshotUrls
                            })
                        }
                    )
                } else {
                    return false
                }
            })
        }

        handleBack() {
            this.$router.back() // TODO: добавить backUrl в параметрах и возвращаться по нему, если нет - то на главную
        }
    }
</script>

<style scoped>

</style>