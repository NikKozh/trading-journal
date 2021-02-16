<template>
    <div id="stats-view">
        <el-row>
            <h1>За всё время</h1>
                <template v-if="!!allTimeStats">
                    <p>Сумма: {{ allTimeStats.incomeF() }}</p>
                    <p>Всего сделок: {{ allTimeStats.contractsCount }}</p>
                    <p>Сделки +: {{ allTimeStats.winningContracts }}</p>
                    <p>Сделки -: {{ allTimeStats.loosedContracts() }}</p>
                    <p>% успешных: {{ allTimeStats.winRatePercentF() }}</p>
                </template>
            <el-divider></el-divider>
            <el-col :span="8" style="padding-right: 5px;">
                <h1>По дням</h1>
                <el-table class="stats-table"
                          :data="dailyItems"
                          :row-class-name="tableRowClass"
                          :empty-text="dailyLoadingText"
                          border
                >
                    <el-table-column prop="day" label="Дата">
                        <template slot-scope="scope">
                            <i class="el-icon-time"/>
                            <span>
                                {{ dayColFormatter(scope.row) }}
                            </span>
                        </template>
                    </el-table-column>
                    <el-table-column prop="income"
                                     label="Сумма за день"
                                     :formatter="incomeFormatter"
                    ></el-table-column>
                    <el-table-column prop="contractsCount" label="Всего сделок"></el-table-column>
                    <el-table-column prop="winningContracts" label="Сделки +" width="80"></el-table-column>
                    <el-table-column prop="loosedContracts"
                                     label="Сделки -"
                                     width="80"
                                     :formatter="loosedContractsFormatter"
                    ></el-table-column>
                    <el-table-column prop="winRatePercent"
                                     label="% успешных"
                                     :formatter="winRateFormatter"
                    ></el-table-column>
                </el-table>
            </el-col>
            <el-col :span="8" style="padding: 0 5px;">
                <h1>По неделям</h1>
                <el-table class="stats-table"
                          :data="weeklyItems"
                          :row-class-name="weeklyTableRowClass"
                          :empty-text="weeklyLoadingText"
                          border
                >
                    <el-table-column prop="dayRange"
                                     label="Диапазон дней"
                                     width="115"
                    >
                        <template slot-scope="scope">
                            <i class="el-icon-time"/>
                            <span>
                                {{ weeklyDaysColFormatter(scope.row) }}
                            </span>
                        </template>
                    </el-table-column>
                    <el-table-column prop="income"
                                     label="Сумма за неделю"
                                     :formatter="weeklyIncomeFormatter"
                    ></el-table-column>
                    <el-table-column prop="contractsCount" label="Всего сделок" width="105"></el-table-column>
                    <el-table-column prop="winningContracts" label="Сделки +" width="80"></el-table-column>
                    <el-table-column prop="loosedContracts"
                                     label="Сделки -"
                                     width="80"
                                     :formatter="weeklyLoosedContractsFormatter"
                    ></el-table-column>
                    <el-table-column prop="winRatePercent"
                                     label="% успешных"
                                     width="105"
                                     :formatter="weeklyWinRateFormatter"
                    ></el-table-column>
                </el-table>
            </el-col>
            <el-col :span="8" style="padding-left: 5px">
                <h1>По месяцам</h1>
                <el-table class="stats-table"
                          :data="yearlyItems"
                          :row-class-name="yearlyTableRowClass"
                          :empty-text="yearlyLoadingText"
                          border
                >
                    <el-table-column prop="monthYear"
                                     label="Месяц и год">
                        <template slot-scope="scope">
                            <i class="el-icon-time"/>
                            <span class="capitalize">
                                {{ yearlyDaysColFormatter(scope.row) }}
                            </span>
                        </template>
                    </el-table-column>
                    <el-table-column prop="income"
                                     label="Сумма за месяц"
                                     :formatter="yearlyIncomeFormatter"
                    ></el-table-column>
                    <el-table-column prop="contractsCount" label="Всего сделок" width="105"></el-table-column>
                    <el-table-column prop="winningContracts" label="Сделки +" width="80"></el-table-column>
                    <el-table-column prop="loosedContracts"
                                     label="Сделки -"
                                     width="80"
                                     :formatter="yearlyLoosedContractsFormatter"
                    ></el-table-column>
                    <el-table-column prop="winRatePercent"
                                     label="% успешных"
                                     width="105"
                                     :formatter="yearlyWinRateFormatter"
                    ></el-table-column>
                </el-table>
            </el-col>
        </el-row>
    </div>
</template>

<script lang="ts">
    import Vue from "vue"
    import {Component} from "vue-property-decorator";
    import DailyStatsItem from "../models/DailyStatsItem";
    import {defaultActionOnError, fetchAndResolve, fetchAndResolveArray} from "../utils/apiJsonResolver";
    import ApiRoutes from "../router/ApiRoutes";
    import WeeklyStatsItem from "../models/WeeklyStatsItem";
    import YearlyStatsItem from "../models/YearlyStatsItem";
    import AllTimeStats from "../models/AllTimeStats";
    import Contract from "../models/Contract";

    @Component
    export default class Stats extends Vue {
        allTimeStats: AllTimeStats | null = null

        dailyItems: DailyStatsItem[] = []
        weeklyItems: WeeklyStatsItem[] = []
        yearlyItems: WeeklyStatsItem[] = []

        dailyLoadingText: string = "Данные загружаются..."
        weeklyLoadingText: string = "Данные загружаются..."
        yearlyLoadingText: string = "Данные загружаются..."

        created() {
            const fetchYearly = () => fetchAndResolveArray(
                ApiRoutes.yearlyStats,
                YearlyStatsItem,
                (yearlyItems: YearlyStatsItem[]) => {
                    console.log("DONE: ", yearlyItems)
                    this.yearlyItems = yearlyItems
                },
                defaultActionOnError(_ => this.yearlyLoadingText = "Произошла ошибка при загрузке данных!")
            )
            const fetchWeekly = () => fetchAndResolveArray(
                ApiRoutes.weeklyStats,
                WeeklyStatsItem,
                (weeklyItems: WeeklyStatsItem[]) => {
                    console.log("DONE: ", weeklyItems)
                    this.weeklyItems = weeklyItems
                    fetchYearly()
                },
                defaultActionOnError(_ => this.weeklyLoadingText = "Произошла ошибка при загрузке данных!")
            )
            const fetchDaily = () => fetchAndResolveArray(
                ApiRoutes.dailyStats,
                DailyStatsItem,
                (dailyItems: DailyStatsItem[]) => {
                    console.log("DONE: ", dailyItems)
                    this.dailyItems = dailyItems
                    fetchWeekly()
                },
                defaultActionOnError(_ => this.dailyLoadingText = "Произошла ошибка при загрузке данных!")
            )
            fetchAndResolve(
                ApiRoutes.allTimeStats,
                AllTimeStats,
                (allTimeStats: AllTimeStats) => {
                    console.log("DONE: ", allTimeStats)
                    this.allTimeStats = allTimeStats
                    fetchDaily()
                },
                defaultActionOnError()
            )
        }


        dayColFormatter(row: DailyStatsItem): string {
            return row.dayF()
        }

        incomeFormatter(row: DailyStatsItem): string {
            return row.incomeF()
        }

        loosedContractsFormatter(row: DailyStatsItem): string {
            return row.loosedContractsF()
        }

        winRateFormatter(row: DailyStatsItem): string {
            return row.winRatePercentF()
        }

        tableRowClass({row}: {row: DailyStatsItem, rowIndex: number}): string {
            return row.income >= 0 ? "success-row" : "fail-row"
        }


        weeklyDaysColFormatter(row: WeeklyStatsItem): string {
            return row.dayFromF() + " - " + row.dayToF()
        }

        weeklyIncomeFormatter(row: DailyStatsItem): string {
            return row.incomeF()
        }

        weeklyLoosedContractsFormatter(row: WeeklyStatsItem): string {
            return row.loosedContractsF()
        }

        weeklyWinRateFormatter(row: WeeklyStatsItem): string {
            return row.winRatePercentF()
        }

        weeklyTableRowClass({row}: {row: WeeklyStatsItem, rowIndex: number}): string {
            return row.income >= 0 ? "success-row" : "fail-row"
        }


        yearlyDaysColFormatter(row: YearlyStatsItem): string {
            return row.firstMonthDayF("LLLL yyyy")
        }

        yearlyIncomeFormatter(row: DailyStatsItem): string {
            return row.incomeF()
        }

        yearlyLoosedContractsFormatter(row: YearlyStatsItem): string {
            return row.loosedContractsF()
        }

        yearlyWinRateFormatter(row: YearlyStatsItem): string {
            return row.winRatePercentF()
        }

        yearlyTableRowClass({row}: {row: YearlyStatsItem, rowIndex: number}): string {
            return row.income >= 0 ? "success-row" : "fail-row"
        }
    }
</script>

<style>
    span.capitalize {
        text-transform: capitalize;
    }
    .el-table .success-row {
        background: rgb(240, 249, 235);
    }
    .el-table .fail-row {
        background: rgb(254, 240, 240);
    }
</style>