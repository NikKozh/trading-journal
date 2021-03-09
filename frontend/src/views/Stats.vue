<template>
    <div id="stats-view">
        <el-row>
            <h1>За всё время</h1>
                <template v-if="!!allTimeStats">
                    <p>Сумма: {{ getIncome(allTimeStats) }}</p>
                    <p>Всего сделок: {{ allTimeStats.contractsCount }}</p>
                    <p>Сделки +: {{ allTimeStats.winningContracts }}</p>
                    <p>Сделки -: {{ getLoosedContracts(allTimeStats) }}</p>
                    <p>% успешных: {{ getWinratePercent(allTimeStats) }}</p>
                </template>
            <el-divider></el-divider>
            <el-col :span="8" style="padding-right: 5px;">
                <h1>По дням</h1>
                <el-table class="stats-table"
                          :data="dailyItems"
                          :row-class-name="getTableRowClass"
                          :empty-text="dailyLoadingText"
                          border
                >
                    <el-table-column prop="day" label="Дата" :formatter="getDailyDate"></el-table-column>
                    <el-table-column prop="income"
                                     label="Сумма за день"
                                     :formatter="getIncome"
                    ></el-table-column>
                    <el-table-column prop="contractsCount" label="Всего сделок"></el-table-column>
                    <el-table-column prop="winningContracts" label="Сделки +" width="80"></el-table-column>
                    <el-table-column prop="loosedContracts"
                                     label="Сделки -"
                                     width="80"
                                     :formatter="getLoosedContracts"
                    ></el-table-column>
                    <el-table-column prop="winRatePercent"
                                     label="% успешных"
                                     :formatter="getWinratePercent"
                    ></el-table-column>
                </el-table>
            </el-col>
            <el-col :span="8" style="padding: 0 5px;">
                <h1>По неделям</h1>
                <el-table class="stats-table"
                          :data="weeklyItems"
                          :row-class-name="getTableRowClass"
                          :empty-text="weeklyLoadingText"
                          border
                >
                    <el-table-column prop="dayRange" label="Диапазон дней" width="115" :formatter="getWeeklyDate">
                    </el-table-column>
                    <el-table-column prop="income"
                                     label="Сумма за неделю"
                                     :formatter="getIncome"
                    ></el-table-column>
                    <el-table-column prop="contractsCount" label="Всего сделок" width="105"></el-table-column>
                    <el-table-column prop="winningContracts" label="Сделки +" width="80"></el-table-column>
                    <el-table-column prop="loosedContracts"
                                     label="Сделки -"
                                     width="80"
                                     :formatter="getLoosedContracts"
                    ></el-table-column>
                    <el-table-column prop="winRatePercent"
                                     label="% успешных"
                                     width="105"
                                     :formatter="getWinratePercent"
                    ></el-table-column>
                </el-table>
            </el-col>
            <el-col :span="8" style="padding-left: 5px">
                <h1>По месяцам</h1>
                <el-table class="stats-table"
                          :data="monthlyItems"
                          :row-class-name="getTableRowClass"
                          :empty-text="monthlyLoadingText"
                          border
                >
                    <el-table-column prop="monthYear"
                                     class-name="capitalize"
                                     label="Месяц и год"
                                     :formatter="getMonthlyDate"
                    ></el-table-column>
                    <el-table-column prop="income"
                                     label="Сумма за месяц"
                                     :formatter="getIncome"
                    ></el-table-column>
                    <el-table-column prop="contractsCount" label="Всего сделок" width="105"></el-table-column>
                    <el-table-column prop="winningContracts" label="Сделки +" width="80"></el-table-column>
                    <el-table-column prop="loosedContracts"
                                     label="Сделки -"
                                     width="80"
                                     :formatter="getLoosedContracts"
                    ></el-table-column>
                    <el-table-column prop="winRatePercent"
                                     label="% успешных"
                                     width="105"
                                     :formatter="getWinratePercent"
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
    import MonthlyStatsItem from "../models/MonthlyStatsItem";
    import AllTimeStats from "../models/AllTimeStats";
    import AllStats from "../models/AllStats"
    import * as StatsHelper from "../utils/StatsHelper"

    @Component
    export default class Stats extends Vue {
        allTimeStats: AllTimeStats | null = null

        dailyItems: DailyStatsItem[] = []
        weeklyItems: WeeklyStatsItem[] = []
        monthlyItems: MonthlyStatsItem[] = []

        dailyLoadingText: string = "Данные загружаются..."
        weeklyLoadingText: string = "Данные загружаются..."
        monthlyLoadingText: string = "Данные загружаются..."

        created() {
            fetchAndResolve(
                ApiRoutes.allStats,
                AllStats,
                (allStats: AllStats) => {
                    console.log("DONE: ", allStats)
                    this.allTimeStats = allStats.allTime
                    this.dailyItems = allStats.daily
                    this.weeklyItems = allStats.weekly
                    this.monthlyItems = allStats.monthly
                },
                defaultActionOnError()
            )
        }

        // TODO: переиспользовать обобщённые методы для таблицы сделок и заодно мб вынести куда-то:

        getIncome<T extends { income: number }>(statsItem: T): string {
            return StatsHelper.getIncome(statsItem.income)
        }

        getLoosedContracts<T extends { contractsCount: number, winningContracts: number }>(statsItem: T): string {
            return String(StatsHelper.getLoosedContracts(statsItem.contractsCount, statsItem.winningContracts))
        }

        getWinratePercent<T extends { contractsCount: number, winningContracts: number }>(statsItem: T): string {
            return String(StatsHelper.getWinratePercent(statsItem.contractsCount, statsItem.winningContracts)) + "%"
        }

        getTableRowClass<T extends { row: { income: number }, rowIndex: number }>({row}: T): string {
            return row.income >= 0 ? "success-row" : "fail-row"
        }


        getDailyDate(item: DailyStatsItem): string {
            return StatsHelper.getDate(item.day)
        }

        getWeeklyDate(item: WeeklyStatsItem): string {
            return StatsHelper.getDate(item.dayFrom) + " - " + StatsHelper.getDate(item.dayTo)
        }

        getMonthlyDate(item: MonthlyStatsItem): string {
            return StatsHelper.getDate(item.firstMonthDay, "LLLL yyyy")
        }
    }
</script>

<style>
    td.capitalize .cell {
        text-transform: capitalize;
    }
    .el-table .success-row {
        background: rgb(240, 249, 235);
    }
    .el-table .fail-row {
        background: rgb(254, 240, 240);
    }
    .el-table .cell {
        word-break: break-word;
    }
</style>