import {Property as CodecProperty} from "@orchestrator/gen-io-ts"
import AllTimeStats from "./AllTimeStats"
import DailyStatsItem from "./DailyStatsItem"
import WeeklyStatsItem from "./WeeklyStatsItem"
import MonthlyStatsItem from "./MonthlyStatsItem"

export default class AllStats {
    @CodecProperty({ isRequired: true, type: AllTimeStats })
    allTime: AllTimeStats

    @CodecProperty({ isRequired: true })
    daily: DailyStatsItem[]

    @CodecProperty({ isRequired: true })
    weekly: WeeklyStatsItem[]

    @CodecProperty({ isRequired: true })
    monthly: MonthlyStatsItem[]

    constructor(allTime: AllTimeStats, daily: DailyStatsItem[], weekly: WeeklyStatsItem[], monthly: MonthlyStatsItem[]) {
        this.allTime = allTime
        this.daily = daily
        this.weekly = weekly
        this.monthly = monthly
    }
}