import {Option} from "fp-ts/es6/Option"
import * as O from "fp-ts/es6/Option"
import {Property as CodecProperty} from "@orchestrator/gen-io-ts/lib/property";

class CustomError {
    @CodecProperty({ isRequired: true })
    caption: string

    @CodecProperty({ isRequired: true })
    cause: string

    @CodecProperty({ type: String })
    details: Option<string>

    constructor(caption: string, cause: string, details?: string) {
        this.caption = caption
        this.cause = cause
        this.details = O.fromNullable(details)
    }
}