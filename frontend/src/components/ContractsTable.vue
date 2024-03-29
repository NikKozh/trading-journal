<template>
    <div id="contracts-table">
        <h1>Список сделок</h1>
        <el-table :data="contracts"
                  style="width: 100%"
                  :row-class-name="tableRowClass"
                  height="calc(100% - 67px)"
                  :empty-text="emptyText"
                  :default-sort="{ prop: 'created', order: 'descending' }"
        >
            <el-table-column prop="number" label="№" width="50"></el-table-column>
            <el-table-column prop="contractType" label="Счёт" width="80"></el-table-column>
            <el-table-column prop="created"
                             label="Дата"
                             sortable
                             :sort-orders="['ascending', 'descending']"
                             width="120"
            >
                <template slot-scope="scope">
                    <i class="el-icon-time"/>
                    <span style="margin-left: 5px">
                        {{ createdColFormatter(scope.row) }}
                    </span>
                </template>
            </el-table-column>
            <el-table-column prop="expiration"
                             label="Экспирация"
                             :formatter="expirationColFormatter"
                             width="100"
            ></el-table-column>
            <el-table-column prop="fxSymbol" label="Актив" width="80"></el-table-column>
            <el-table-column prop="direction" label="Прогноз" width="80"></el-table-column>
            <el-table-column prop="buyPrice"
                             label="Вложение"
                             :formatter="buyPriceColFormatter"
                             width="90"
            ></el-table-column>
            <el-table-column prop="profitPercent"
                             label="%"
                             :formatter="profitPercentColFormatter"
                             width="80"
            ></el-table-column>
            <el-table-column prop="isWin"
                             label="Результат"
                             :formatter="isWinColFormatter"
                             width="90"
            ></el-table-column>
            <el-table-column prop="income"
                             label="Доход"
                             :formatter="incomeColFormatter"
                             width="80"
            ></el-table-column>
            <el-table-column prop="tags" label="Тэги" min-width="200"></el-table-column>
            <el-table-column prop="isCorrect"
                             label="По ТС?"
                             :formatter="isCorrectColFormatter"
                             width="70"
            ></el-table-column>
            <el-table-column prop="actions" label="Действия" width="180">
                <template slot-scope="scope">
                    <el-tooltip effect="dark" content="Информация о сделке" placement="top">
                        <el-button
                                   icon="el-icon-document"
                                   circle
                                   @click="handleContractOpen(scope.row)"
                        ></el-button>
                    </el-tooltip>
                    <el-tooltip effect="dark" content="Редактировать" placement="top">
                        <el-button type="primary"
                                   icon="el-icon-edit"
                                   circle
                                   @click="handleContractEdit(scope.row)"
                        ></el-button>
                    </el-tooltip>
                    <el-tooltip effect="dark" content="Удалить" placement="top">
                        <el-button type="danger"
                                   icon="el-icon-delete"
                                   circle
                                   @click="handleContractDelete(scope.row)"
                        ></el-button>
                    </el-tooltip>
                </template>
            </el-table-column>
        </el-table>
    </div>
</template>

<script lang="ts">
    import Vue from "vue"
    import {Component} from "vue-property-decorator"
    import {defaultActionOnError, fetchAndResolveArray, submitWithRecovery} from "../utils/apiJsonResolver"
    import ApiRoutes from "../router/ApiRoutes"
    import DetailedError from "../models/DetailedError"
    import Contract from "../models/Contract"
    import EventBus from "../utils/EventBus"
    import Routes from "../router/Routes"
    import goToPage from "../router/goToPage";

    @Component
    export default class ContractsTable extends Vue {
        contracts: Contract[] = []

        emptyText: string = "Данные загружаются..."

        private loadContractsData() {
            fetchAndResolveArray(
                ApiRoutes.contractList,
                Contract,
                (contracts: Contract[]) => {
                    console.log("DONE: ", contracts)
                    this.contracts = contracts
                },
                defaultActionOnError(_ => this.emptyText = "Произошла ошибка при загрузке данных!")
            )
        }

        created() {
            this.loadContractsData()
        }

        // FORMATTERS START ---------------------------------

        buyPriceColFormatter(row: Contract): string {
            return row.buyPriceF()
        }

        profitPercentColFormatter(row: Contract): string {
            return row.profitPercentF()
        }

        isWinColFormatter(row: Contract): string {
            return row.isWinF()
        }

        isCorrectColFormatter(row: Contract): string {
            return row.isCorrectF()
        }

        incomeColFormatter(row: Contract): string {
            return row.incomeF()
        }

        expirationColFormatter(row: Contract): string {
            return row.expirationF()
        }

        createdColFormatter(row: Contract): string {
            return row.createdF()
        }

        // FORMATTERS END ---------------------------------

        tableRowClass({row}: {row: Contract, rowIndex: number}): string {
            return row.isWin ? "success-row" : "fail-row"
        }

        // HANDLERS START ---------------------------------

        handleContractOpen(contract: Contract) {
            goToPage.contractCard(contract.id)
        }

        handleContractEdit(contract: Contract) {
            goToPage.contractForm(contract.id)
        }

        handleContractDelete(contract: Contract) {
            this.$confirm(`Вы точно хотите удалить сделку №${contract.number} от ${contract.createdF()}?`, {
                confirmButtonText: "Да",
                cancelButtonText: "Отмена",
                type: "warning"
            }).then(() => {
                submitWithRecovery(
                    ApiRoutes.deleteContract(contract.id),
                    () => {
                        this.loadContractsData()
                        this.$message({
                            type: "success",
                            message: "Сделка удалена"
                        })
                    }
                )
            })
        }

        // HANDLERS END ---------------------------------
    }
</script>

<style>
    #contracts-table .el-table .success-row {
        background: rgb(240, 249, 235);
    }
    #contracts-table .el-table .fail-row {
        background: rgb(254, 240, 240);
    }
</style>