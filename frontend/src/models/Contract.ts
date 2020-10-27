import {Property as CodecProperty} from "@orchestrator/gen-io-ts"
import * as O from "fp-ts/es6/Option"
import {Option} from "fp-ts/es6/Option"
import * as A from "fp-ts/es6/Array"
import {pipe} from "fp-ts/es6/pipeable"
import {Do} from "fp-ts-contrib/es6/Do"
import {formatBoolean, formatDate, formatFloat, formatMoney, formatOptional, formatPercent} from "../utils/Formatters"
import {flow} from "fp-ts/es6/function"

export default class Contract {
    @CodecProperty({ isRequired: true })
    id: string

    @CodecProperty({ isRequired: true })
    number: number

    @CodecProperty({ isRequired: true })
    contractType: string

    @CodecProperty({ isRequired: true, type: Number })
    created: number

    createdF(formatString?: string): string {
        return formatDate(this.created, formatString)
    }

    @CodecProperty({ isRequired: true })
    expiration: number

    expirationF(): string {
        return String(this.expiration) + " мин."
    }

    @CodecProperty({ isRequired: true })
    fxSymbol: string

    @CodecProperty({ isRequired: true })
    direction: string

    @CodecProperty({ isRequired: true })
    isWin: boolean

    isWinF(): string {
        return formatBoolean("ПРИБЫЛЬ", "УБЫТОК")(this.isWin)
    }

    @CodecProperty({ isRequired: true, type: String })
    screenshotPaths: Array<string>

    @CodecProperty({ isRequired: true })
    tags: string

    @CodecProperty({ isRequired: true })
    isCorrect: boolean

    isCorrectF(): string {
        return formatBoolean()(this.isCorrect)
    }

    @CodecProperty({ isRequired: true })
    description: string

    @CodecProperty({ type: Number })
    buyPrice: Option<number>

    buyPriceF(rawString: boolean = false): string {
        return formatOptional(rawString ? formatFloat() : formatMoney, "N/A")(this.buyPrice)
    }

    @CodecProperty({ type: Number })
    profitPercent: Option<number>

    profitPercentF(rawString: boolean = false): string {
        function stringCorrecter(percentStr: string): string {
            return percentStr + (rawString ? "" : "%")
        }
        return formatOptional(flow(formatPercent, formatFloat(2, stringCorrecter)), "N/A")(this.profitPercent)
    }

    constructor(id: string,
                number: number,
                contractType: string,
                created: number,
                expiration: number,
                fxSymbol: string,
                direction: string,
                isWin: boolean,
                screenshotPaths: string,
                tags: string,
                isCorrect: boolean,
                description: string,
                buyPrice?: number,
                profitPercent?: number) {
        this.id = id
        this.number = number
        this.contractType = contractType
        this.created = created
        this.expiration = expiration
        this.fxSymbol = fxSymbol
        this.direction = direction
        this.isWin = isWin
        this.screenshotPaths = this.getCorrectScreenshotSrcs(screenshotPaths)
        this.tags = tags
        this.isCorrect = isCorrect
        this.description = description
        this.buyPrice = this.getFixedFloatOpt(buyPrice)
        this.profitPercent = this.getFixedFloatOpt(profitPercent, 4)
    }

    static getEmpty(id: string = "", number: number = 0, created: number = 0): Contract {
        return new this(
            "",
            0,
            "",
            0,
            0,
            "",
            "",
            false,
            "",
            "",
            false,
            ""
        )
    }

    // TODO: временный хак, потом это будет не нужно
    private getCorrectScreenshotSrcs(screenshotPaths: string | undefined): Array<string> {
        return screenshotPaths && screenshotPaths.length > 0 ?
            A.map((base64String: string) => `data:image/png;base64,${base64String}`)
                 (screenshotPaths.split(";"))
            : []
    }

    private getFixedFloatOpt(nullableNumber: number | undefined, fixedTo: number = 2): Option<number> {
        return pipe(nullableNumber,
            O.fromNullable,
            O.map((num: number) => Number.parseFloat(num.toFixed(fixedTo)))
        )
    }

    income(): Option<number> {
        return Do(O.option)
            .bind("price", this.buyPrice)
            .bind("percent", this.profitPercent)
            .return(({ price, percent }) => {
                const float = this.isWin ? price * percent : -price
                return Number.parseFloat(float.toFixed(2))
            })
    }

    incomeF(): string {
        return formatOptional(formatMoney, "N/A")(this.income())
    }
}