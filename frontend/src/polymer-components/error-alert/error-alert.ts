/*import {html, PolymerElement} from "@polymer/polymer/polymer-element";
import {customElement, property} from "@polymer/decorators/lib/decorators";*/
import {LitElement, html, customElement, property, TemplateResult, query, PropertyValues} from "lit-element";
import {pipe} from "fp-ts/es6/pipeable";
import * as O from "fp-ts/es6/Option";
import {Dialog} from "@material/mwc-dialog/mwc-dialog";
import {getPropertyString, varToString} from "../../utils/reflexion";

@customElement("error-alert")
export default class ErrorAlert extends LitElement {
    @property()
    caption: string = "ОШИБКА"

    @property()
    text: string = "Произошла ошибка!"

    @query("#mwc-error-alert")
    mwcDialog!: Dialog

    @property({ reflect: true })
    open = false;

    attributeChangedCallback(name: string, old: string | null, value: string | null): void {
        super.attributeChangedCallback(name, old, value);
        console.log('updated alert: ', name, old, value);

        // TODO: рассмотреть возможность конвертировать идентификатор члена класса в строку, чтобы было безопаснее
        if (name === "open" && value === "true") {
            this.mwcDialog.open = true;
        }
    }

    render(): TemplateResult {
        return html`
            <mwc-dialog id="mwc-error-alert" heading="${this.caption}">
                ${this.text}
                <mwc-button slot="primaryAction" dialogAction="ok">Ок</mwc-button>
            </mwc-dialog>
        `
    }
}