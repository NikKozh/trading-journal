import {Property as CodecProperty} from "@orchestrator/gen-io-ts";
import {formatDate, formatMoney} from "../../utils/Formatters";

export default class DailyStatsItem {
    @CodecProperty({ isRequired: true, type: Number })
    day: number

    @CodecProperty({ isRequired: true })
    income: number

    @CodecProperty({ isRequired: true })
    contractsCount: number

    @CodecProperty({ isRequired: true })
    winningContracts: number

    constructor(day: number, income: number, contractsCount: number, winningContracts: number) {
        this.day = day
        this.income = income
        this.contractsCount = contractsCount
        this.winningContracts = winningContracts
    }
}