<template>
    <div id="create-contract-view">
        <el-page-header @back="handleBack" title="Назад">
            <h1 id="contract-header-text" slot="content">Создать сделку (с нуля)</h1>
        </el-page-header>
        <el-row>
            <el-col :span="14">
                <!--suppress HtmlUnknownTarget -->
                <el-image v-if="contract.screenshotPaths && contract.screenshotPaths.length > 0"
                          :src="contract.screenshotPaths[0]"
                          class="contract-image"
                          id="contract-screenshots-panel"
                          fit="none"
                          :preview-src-list="contract.screenshotPaths"
                ></el-image>
            </el-col>
            <el-col id="contract-details-panel" :span="10">
                <ContractForm :contract="contract"></ContractForm>
            </el-col>
        </el-row>
    </div>
</template>

<script lang="ts">
    import Vue from "vue"
    import {Component} from "vue-property-decorator"
    import ContractForm from "../components/ContractForm.vue"
    import Contract from "../models/Contract"
    import {fetchAndResolve} from "../utils/apiJsonResolver"
    import ApiRoutes from "../router/ApiRoutes"
    import NewContractData from "../models/NewContractData"
    import { getTime } from 'date-fns'

    @Component({
        components: { ContractForm }
    })
    export default class CreateContract extends Vue {
        contract: Contract = Contract.getEmpty()

        created() {
            this.contract.created = getTime(new Date())
            fetchAndResolve(
                ApiRoutes.newContractData,
                NewContractData,
                (newContractData: NewContractData) => {
                    this.contract.id = newContractData.id
                    this.contract.number = newContractData.number
                }
            )
        }

        handleBack() {
            this.$router.back() // TODO: добавить backUrl в параметрах и возвращаться по нему, если нет - то на главную
        }
    }
</script>

<style scoped>

</style>