import {customElement, html, LitElement, property as litProperty, TemplateResult} from "lit-element"
import {pipe} from "fp-ts/es6/pipeable";
import {DetailedError} from "../../models/DetailedError";
import {Option} from "fp-ts/es6/Option";
import * as O from "fp-ts/es6/Option"

@customElement("error-cause-block")
class ErrorCauseBlock extends LitElement {
    @litProperty()
    cause!: string

    @litProperty()
    details: Option<string> = O.none

    private causeBlock(): TemplateResult {
        return pipe(
            this.details,
            O.map(details => html`
                <h4>Детали:</h4>
                <p>${details}</p>
            `),
            O.getOrElse(() => html``)
        )
    }

    render(): TemplateResult {
        return html`
            <div class="error-cause-block">
                <h3>Причина:</h3>
                <p>${this.cause}</p>
                ${this.causeBlock()}
            </div>
        `
    }
}