import {customElement, html, LitElement, property as litProperty, query, TemplateResult} from "lit-element"
import {DetailedError} from "../../models/DetailedError"
import {MwcAppDialog} from "mwc-app-dialog/MwcAppDialog"
import "../error-cause-block/error-cause-block"

@customElement("error-alert")
export class ErrorAlert extends LitElement {
    @litProperty()
    error: DetailedError = new DetailedError("ERROR", "Unknown.")

    @query("#error-dialog")
    errorDialog!: MwcAppDialog

    open(error: DetailedError) {
        const {caption, cause, details} = error

        this.errorDialog.notice(
            caption,
            html`<error-cause-block class="error-cause-block" .cause="${cause}" .details="${details}">
                 </error-cause-block>`
        )
    }

    render(): TemplateResult {
        return html`
            <mwc-app-dialog id="error-dialog"></mwc-app-dialog> 
        `
    }
}