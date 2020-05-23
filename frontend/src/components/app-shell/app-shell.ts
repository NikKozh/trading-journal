import {customElement, html, LitElement, property as litProperty, query, TemplateResult} from "lit-element"
import "mwc-app-dialog"
import {DetailedError} from "../../models/DetailedError";
import {ErrorAlert} from "../error-alert/error-alert"
import {Message} from "../../models/Message";
import {fetchAndResolve} from "../../utils/apiJsonResolver";
import Routes from "../../conf/Routes";

@customElement("app-shell")
class AppShell extends LitElement {
    @litProperty()
    signal: Message = new Message("Loading...", "N/A", 0)

    @query("#error-alert")
    errorAlert!: ErrorAlert

    protected firstUpdated() {
        fetchAndResolve(
            Routes.pingMessage,
            Message,
            (message: Message) => this.signal = message,
            (error: DetailedError) => {
                this.errorAlert.open(error)
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
            <error-alert id="error-alert"></error-alert> 
            <h2>Message: ${this.signal.message}</h2>
            <h2>Status: ${this.signal.status}</h2>
            <h2>Code: ${this.signal.code}</h2>
        `
    }
}