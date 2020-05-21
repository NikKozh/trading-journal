import {LitElement, html, customElement, property as litProperty, TemplateResult, query} from "lit-element"
import Routes from "../../conf/Routes"
import "../../utils/arrayExpansion"
import "mwc-app-dialog"
import {MwcAppDialog} from "mwc-app-dialog/MwcAppDialog"
import {Property as CodecProperty} from "@orchestrator/gen-io-ts"
import {fetchAndResolve} from "../../utils/apiJsonResolver"

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

@customElement("tj-frontend-app")
class TjFrontendApp extends LitElement {
    @litProperty()
    signal: Message = new Message("No message provided yet", "N/A", 0)

    @query("#error-alert")
    errorAlert!: MwcAppDialog

    constructor() {
        super()

        fetchAndResolve(
            Routes.pingMessage,
            Message,
            (message: Message) => this.signal = message,
            (error: Error) => {
                this.errorAlert.notice("Заголовок ошибки", error.message)
                this.signal = new Message("error", "down", -1)
            }
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