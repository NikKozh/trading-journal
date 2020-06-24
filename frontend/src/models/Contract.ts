import {Property as CodecProperty} from "@orchestrator/gen-io-ts"
import * as O from "fp-ts/es6/Option"
import {Option} from "fp-ts/es6/Option"

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
        this.buyPrice = O.fromNullable(buyPrice)
        this.profitPercent = O.fromNullable(profitPercent)
    }
}