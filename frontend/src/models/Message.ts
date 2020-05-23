import {Property as CodecProperty} from "@orchestrator/gen-io-ts";

export class Message {
    @CodecProperty({isRequired: true})
    message: string

    @CodecProperty({isRequired: true})
    status: string

    @CodecProperty({isRequired: true})
    code: number

    constructor(message: string, status: string, code: number) {
        this.message = message
        this.status = status
        this.code = code
    }
}