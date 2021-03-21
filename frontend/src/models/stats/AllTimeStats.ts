import {Property as CodecProperty} from "@orchestrator/gen-io-ts";

export default class AllTimeStats {
    @CodecProperty({isRequired: true})
    income: number

    @CodecProperty({isRequired: true})
    contractsCount: number

    @CodecProperty({isRequired: true})
    winningContracts: number

    constructor(income: number, contractsCount: number, winningContracts: number) {
        this.income = income
        this.contractsCount = contractsCount
        this.winningContracts = winningContracts
    }
}