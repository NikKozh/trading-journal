import {html, PolymerElement} from '@polymer/polymer/polymer-element.js';
import Config from '../config';

/**
 * @customElement
 * @polymer
 */
class TjFrontendApp extends PolymerElement {
    static get template() {
        // language=HTML
        return html`
            <style>
                :host {
                    display: block;
                }
            </style>
            <h2>Message: [[prop1.message]]</h2>
            <h2>Status: [[prop1.status]]</h2>
        `;
    }

    static get properties() {
        return {
            prop1: {
                type: Object,
                value: { "message": "No message provided yet", "status": "N/A" }
            }
        };
    }

    connectedCallback() {
        super.connectedCallback();

        fetch(`${Config.appUrl}/ping`)
            .then(
                r => r.json(),
                err => {
                    return {"message": `Problem on server side. Error: ${err}`, "status": "Down"}
                }
            )
            .then(
                json => this.prop1 = json,
                err => this.prop1 = {"message": `Problem on parsing json. Error: ${err}`, "status": "Down"}
            )
    }
}

window.customElements.define('tj-frontend-app', TjFrontendApp);
