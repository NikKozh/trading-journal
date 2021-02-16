import {Property as CodecProperty} from "@orchestrator/gen-io-ts";
import {formatDate, formatMoney} from "../utils/Formatters";

export default class YearlyStatsItem {
    @CodecProperty({ isRequired: true, type: Number })
    firstMonthDay: number

    firstMonthDayF(formatString?: string): string {
        return formatDate(this.firstMonthDay, formatString)
    }

    @CodecProperty({ isRequired: true })
    income: number

    incomeF(): string {
        return formatMoney(this.income)
    }

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

    loosedContracts(): number {
        return this.contractsCount - this.winningContracts
    }

    loosedContractsF(): string {
        return String(this.loosedContracts())
    }

    winRatePercent(): number {
        const float = (this.winningContracts * 100) / this.contractsCount
        return Number.parseFloat(float.toFixed(2))
    }

    winRatePercentF(): string {
        return String(this.winRatePercent()) + "%"
    }
}