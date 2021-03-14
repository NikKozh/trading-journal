<template>
    <el-form :model="form" ref="form" :rules="rules" label-position="top">
        <el-form-item label="Пока пользователь только один. Введите пароль:" prop="password">
            <el-input v-model="form.password" type="password"></el-input>
            <el-form-item>
                <el-button type="primary" @click="submitForm">Отправить</el-button>
                <el-button @click="handleBack">Отмена</el-button>
            </el-form-item>
        </el-form-item>
    </el-form>
</template>

<script lang="ts">
    import Vue from "vue"
    import {Component} from "vue-property-decorator"
    import {Form} from "element-ui"
    import {submitWithRecovery} from "../utils/apiJsonResolver"
    import ApiRoutes from "../router/ApiRoutes"
    import goToPage from "../router/goToPage"
    import {setAuthCookie} from "../utils/Helper"
    import EventBus from "../utils/EventBus"

    @Component()
    export default class SignIn extends Vue {
        form = { password: "" }

        rules = {
            password: [
                // TODO: обобщить такие однотипные валидаторы
                { required: true, message: 'Это поле обязательно для заполнения', trigger: 'change' }
            ]
        }

        $refs! = {
            form: Form
        }

        submitForm() {
            (this.$refs.form as Form).validate((valid: boolean) => {
                if (valid) {
                    submitWithRecovery(
                        ApiRoutes.signIn,
                        () => {
                            // TODO: храним в открытую в куках, просто отвратительно, но это временное решение
                            setAuthCookie(this.form.password)
                            EventBus.$emit("hide-demo-warning")
                            goToPage.contractList()
                        },
                        undefined, // задействуем дефолтное значение аргумента при ошибке
                        {
                            method: "POST",
                            headers: {
                                'Content-Type': 'application/json'
                            },
                            // TODO: отправляем в открытую и без HTTPS, просто отвратительно, но это временное решение
                            body: JSON.stringify({
                                password: this.form.password
                            })
                        }
                    )
                } else {
                    return false
                }
            })
        }

        handleBack() {
            goToPage.contractCard(this.contract.id)
        }
    }
</script>

<style scoped>

</style>