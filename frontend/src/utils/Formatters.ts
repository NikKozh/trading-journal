import { formatWithOptions } from "date-fns/fp"
import { ru } from "date-fns/locale"
import {pipe} from "fp-ts/es6/pipeable"
import {Option} from "fp-ts/es6/Option"
import * as O from "fp-ts/es6/Option"

export function formatDate(date: Date | number, formatString: string = 'dd.MM.yyyy') {
    return formatWithOptions({ locale: ru }, formatString, date)
}

export function formatBoolean(stringOnTrue: string = "ДА", stringOnFalse: string = "НЕТ") {
    return function (bool: boolean): string {
        return bool ? stringOnTrue : stringOnFalse
    }
}

export function formatFloat(fixedTo: number = 2) {
    return function (float: number): string {
        return float.toFixed(fixedTo)
    }
}

export function formatOptional<A>(mapF: (value: A) => string = String) {
    return function (option: Option<A>): string {
        return pipe(option,
            O.map(mapF),
            O.getOrElse(() => "")
        )
    }
}

export function formatPercent(floatPercent: number): number {
    return floatPercent * 100
}

export function formatMoney(rawAmount: number): string {
    return pipe(rawAmount,
        Math.abs,
        formatFloat(),
        (strAmount: string) => rawAmount < 0 ? `-$${strAmount}` : `$${strAmount}`
    )
}