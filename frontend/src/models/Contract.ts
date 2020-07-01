import {Property as CodecProperty} from "@orchestrator/gen-io-ts"
import * as O from "fp-ts/es6/Option"
import {Option} from "fp-ts/es6/Option"
import {pipe} from "fp-ts/es6/pipeable"
import {Do} from "fp-ts-contrib/es6/Do"

export default class Contract {
    @CodecProperty({ isRequired: true })
    id: string

    @CodecProperty({ isRequired: true })
    number: number

    @CodecProperty({ isRequired: true })
    contractType: string

    @CodecProperty({ isRequired: true, type: Number })
    created: number // TODO: timestamp

    @CodecProperty({ isRequired: true })
    expiration: number

    @CodecProperty({ isRequired: true })
    fxSymbol: string

    @CodecProperty({ isRequired: true })
    direction: string

    @CodecProperty({ isRequired: true })
    isWin: boolean

    @CodecProperty({ isRequired: true })
    screenshotPaths: string

    @CodecProperty({ isRequired: true })
    tags: string

    @CodecProperty({ isRequired: true })
    isCorrect: boolean

    @CodecProperty({ isRequired: true })
    description: string

    @CodecProperty({ type: Number })
    buyPrice: Option<number>

    @CodecProperty({ type: Number })
    profitPercent: Option<number>

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
        this.screenshotPaths = screenshotPaths
        this.tags = tags
        this.isCorrect = isCorrect
        this.description = description
        this.buyPrice = this.getFixedFloatOpt(buyPrice)
        this.profitPercent = this.getFixedFloatOpt(profitPercent, 4)
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
            .return(({price, percent}) => {
                const float = this.isWin ? price * percent : -price
                return Number.parseFloat(float.toFixed(2))
            })
    }
}