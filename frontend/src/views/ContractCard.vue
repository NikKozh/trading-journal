<template>
    <div id="contract-details">
        <el-page-header @back="handleBack" title="Назад">
            <h1 slot="content" style="padding: 0">{{ headerContent }}</h1>
        </el-page-header>
        <template v-if="!!contract">
            <el-row>
                <el-col :span="14">
                    <!--suppress HtmlUnknownTarget -->
                    <el-image v-if="contract.screenshotPaths.length > 0"
                              :src="contract.screenshotPaths[0]"
                              class="contract-image"
                              style="width: 100%; height: 916px"
                              fit="none"
                              :preview-src-list="contract.screenshotPaths"
                    ></el-image>
                </el-col>
                <el-col :span="10" style="padding-left: 20px; padding-top: 42px">
                    <ContractView v-if="this.$route.params.mode === 'view'" :contract="contract"></ContractView>
                    <ContractForm v-if="this.$route.params.mode === 'edit'" :contract="contract"></ContractForm>
                </el-col>
            </el-row>
        </template>
    </div>
</template>

<script lang="ts">
    import Vue from "vue"
    import {Component} from "vue-property-decorator"
    import Contract from "../models/Contract"
    import {fetchAndResolve} from "../utils/apiJsonResolver"
    import ApiRoutes from "../router/ApiRoutes"
    import DetailedError from "../models/DetailedError"
    import EventBus from "../utils/EventBus"
    import ContractForm from "../components/ContractForm.vue"
    import ContractView from "../components/ContractView.vue"

    @Component({
        components: { ContractForm, ContractView }
    })
    export default class ContractCard extends Vue {
        contract: Contract | null = null

        headerContent: string = "Данные загружаются..."

        created() {
            // TODO: добавить проверку на this.$route.params.mode и NotFound в случае левого мода (или сделать это на уровне роутера)
            // TODO: вынести запрос к серверу в watcher
            fetchAndResolve(
                ApiRoutes.contractCard(this.$route.params.id),
                Contract,
                (contract: Contract) => {
                    this.contract = contract
                    this.headerContent = `Сделка №${this.contract.number} от ${this.contract.createdF()}` // TODO: вынести в watcher?
                },
                (error: DetailedError) => {
                    console.log("ERROR: ", error)
                    this.headerContent = "ОШИБКА"
                    EventBus.$emit("error-occurred", error)
                }
            )
        }

        handleBack() {
            this.$router.back() // TODO: добавить backUrl в параметрах и возвращаться по нему, если нет - то на главную
        }
    }
</script>

<style>
    #contract-details .contract-image img {
        object-position: right top;
    }
</style>