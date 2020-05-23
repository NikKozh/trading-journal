import {customElement, html, LitElement, property as litProperty, query, TemplateResult} from "lit-element"
import "mwc-app-dialog"
import {MwcAppDialog} from "mwc-app-dialog/MwcAppDialog"
import * as resolver from "../../utils/apiJsonResolver"
import {pipe} from "fp-ts/es6/pipeable";
import * as TE from "fp-ts/es6/TaskEither"
import * as E from "fp-ts/es6/Either";
import {DetailedError} from "../../models/DetailedError";
import {ErrorAlert} from "../error-alert/error-alert"
import {Message} from "../../models/Message";

@customElement("app-shell")
class AppShell extends LitElement {
    @litProperty()
    signal: Message = new Message("No message provided yet", "N/A", 0)

    @query("#error-alert")
    errorAlert!: ErrorAlert

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
            resolver.extractModel(DetailedError),
            resolver.resolveModel(
                errorModel => console.log("Success error model: ", errorModel),
                error => {
                    console.log("THIS errorAlert: ", this.errorAlert)
                    const detailedError = new DetailedError("Заголовок ошибки", error.message, "Детали")
                    this.errorAlert.open(detailedError)
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
            <error-alert id="error-alert"></error-alert> 
            <h2>Message: ${this.signal.message}</h2>
            <h2>Status: ${this.signal.status}</h2>
            <h2>Code: ${this.signal.code}</h2>
        `
    }
}