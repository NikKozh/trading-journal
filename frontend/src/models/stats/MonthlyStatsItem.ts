import {Property as CodecProperty} from "@orchestrator/gen-io-ts";
import {formatDate, formatMoney} from "../../utils/Formatters";

export default class MonthlyStatsItem {
    @CodecProperty({ isRequired: true, type: Number })
    firstMonthDay: number

    @CodecProperty({ isRequired: true })
    income: number

    @CodecProperty({ isRequired: true })
    contractsCount: number

    @CodecProperty({ isRequired: true })
    winningContracts: number

    constructor(firstMonthDay: number, income: number, contractsCount: number, winningContracts: number) {
        this.firstMonthDay = firstMonthDay
        this.income = income
        this.contractsCount = contractsCount
        this.winningContracts = winningContracts
    }
}