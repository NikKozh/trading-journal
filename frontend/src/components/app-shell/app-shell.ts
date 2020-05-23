import {
    LitElement,
    html,
    customElement,
    property as litProperty,
    TemplateResult,
    query
} from "lit-element"
import Routes from "../../conf/Routes"
import "mwc-app-dialog"
import {MwcAppDialog} from "mwc-app-dialog/MwcAppDialog"
import {Property as CodecProperty} from "@orchestrator/gen-io-ts"
import {fetchAndResolve} from "../../utils/apiJsonResolver"
import {pipe} from "fp-ts/es6/pipeable";
import * as resolver from "../../utils/apiJsonResolver"
import {CustomError} from "../error/error"
import * as TE from "fp-ts/es6/TaskEither"
import * as Task from "fp-ts/es6/Task";
import * as E from "fp-ts/es6/Either";

class Message {
    @CodecProperty({ isRequired: true })
    message: string

    @CodecProperty({ isRequired: true })
    status: string

    @CodecProperty({ isRequired: true })
    code: number

    constructor(message: string, status: string, code: number) {
        this.message = message
        this.status = status
        this.code = code
    }
}

@customElement("app-shell")
class AppShell extends LitElement {
    @litProperty()
    signal: Message = new Message("No message provided yet", "N/A", 0)

    @query("#error-alert")
    errorAlert!: MwcAppDialog

    protected firstUpdated() {
        /*        fetchAndResolve(
            Routes.pingMessage,
            Message,
            (message: Message) => this.signal = message,
            (error: Error) => {
                this.errorAlert.notice("Заголовок ошибки", error.message)
                this.signal = new Message("error", "down", -1)
            }
        )*/

        const obj = { caption: "123", cause: "Cause", details: 123 }
        const objTE = TE.fromEither(E.right(obj))

        pipe(objTE,
            resolver.extractModel(CustomError),
            resolver.resolveModel(
                errorModel => console.log("Success error model: ", errorModel),
                error => {
                    console.log("THIS errorAlert: ", this.errorAlert)
                    this.errorAlert.notice("Заголовок ошибки", error.message)
                }
            )
        )
    }

    render(): TemplateResult {
        return html`
            <style>
                :host {
                    display: block
                }
                #error-alert {
                    --mdc-theme-surface: #ffe0e0
                }
            </style>
            <mwc-app-dialog id="error-alert"></mwc-app-dialog> 
            <h2>Message: ${this.signal.message}</h2>
            <h2>Status: ${this.signal.status}</h2>
            <h2>Code: ${this.signal.code}</h2>
        `
    }
}