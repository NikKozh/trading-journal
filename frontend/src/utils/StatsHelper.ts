// TODO: возможно, такой специфике здесь не место и нужно перенести куда-то отдельно
import {formatDate, formatMoney} from "./Formatters";

export function getIncome(raw: number): string {
    return formatMoney(raw)
}

export function getLoosedContracts(allContracts: number, winningContracts: number): number {
    return allContracts - winningContracts
}

export function getWinratePercent(allContracts: number, winningContracts: number): number {
    const float = (winningContracts * 100) / allContracts
    return Number.parseFloat(float.toFixed(2))
}

export function getDate(raw: number, formatString?: string): string {
    return formatDate(raw, formatString)
}