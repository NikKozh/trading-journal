import {Property as CodecProperty} from "@orchestrator/gen-io-ts";
import {formatDate, formatMoney} from "../utils/Formatters";

export default class WeeklyStatsItem {
    @CodecProperty({ isRequired: true, type: Number })
    dayFrom: number

    @CodecProperty({ isRequired: true, type: Number })
    dayTo: number

    @CodecProperty({ isRequired: true })
    income: number

    @CodecProperty({ isRequired: true })
    contractsCount: number

    @CodecProperty({ isRequired: true })
    winningContracts: number

    constructor(dayFrom: number, dayTo: number, income: number, contractsCount: number, winningContracts: number) {
        this.dayFrom = dayFrom
        this.dayTo = dayTo
        this.income = income
        this.contractsCount = contractsCount
        this.winningContracts = winningContracts
    }
}