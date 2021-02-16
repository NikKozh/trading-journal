import {Property as CodecProperty} from "@orchestrator/gen-io-ts";
import {formatDate, formatMoney} from "../utils/Formatters";

export default class AllTimeStats {
    @CodecProperty({ isRequired: true })
    income: number

    incomeF(): string {
        return formatMoney(this.income)
    }

    @CodecProperty({ isRequired: true })
    contractsCount: number

    @CodecProperty({ isRequired: true })
    winningContracts: number

    constructor(income: number, contractsCount: number, winningContracts: number) {
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