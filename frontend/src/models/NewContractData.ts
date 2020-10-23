import {Property as CodecProperty} from "@orchestrator/gen-io-ts";

export default class NewContractData {
    @CodecProperty({ isRequired: true })
    id: string

    @CodecProperty({ isRequired: true })
    number: number

    constructor(id: string, number: number) {
        this.id = id
        this.number = number
    }
}