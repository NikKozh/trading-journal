var __decorate = (this && this.__decorate) || function (decorators, target, key, desc) {
    var c = arguments.length, r = c < 3 ? target : desc === null ? desc = Object.getOwnPropertyDescriptor(target, key) : desc, d;
    if (typeof Reflect === "object" && typeof Reflect.decorate === "function") r = Reflect.decorate(decorators, target, key, desc);
    else for (var i = decorators.length - 1; i >= 0; i--) if (d = decorators[i]) r = (c < 3 ? d(r) : c > 3 ? d(target, key, r) : d(target, key)) || r;
    return c > 3 && r && Object.defineProperty(target, key, r), r;
};
import { html, PolymerElement } from "@polymer/polymer/polymer-element";
import { customElement, property } from "@polymer/decorators/lib/decorators";
import * as t from "io-ts";
import * as TE from "fp-ts/es6/TaskEither";
import * as E from "fp-ts/es6/Either";
import * as T from "fp-ts/es6/Task";
import Config from '../conf/Config';
import { pipe } from "fp-ts/es6/pipeable";
import { flow } from "fp-ts/es6/function";
const Message = t.type({
    message: t.string,
    status: t.string
});
let TjFrontendApp = class TjFrontendApp extends PolymerElement {
    constructor() {
        super(...arguments);
        this.signal = { message: "No message provided yet", status: "N/A" };
    }
    static get template() {
        return html `
            <style>
                :host {
                    display: block;
                }
            </style>
            <h2>Message: [[signal.message]]</h2>
            <h2>Status: [[signal.status]]</h2>
        `;
    }
    fetchMessage(url) {
        return TE.tryCatch(() => {
            console.log("STEP: START FETCHING...");
            return fetch(url);
        }, flow(String, Error));
    }
    getJsonObject(responseTE) {
        return TE.chain((response) => {
            return TE.tryCatch(() => {
                console.log("STEP: GETTING JSON...");
                return response.json();
            }, flow(String, Error));
        })(responseTE);
    }
    extractMessage(jsonObjectTE) {
        /* Сначала проверяем, что нет лишних полей */
        function validateMessage() {
            // not implemented yet
        }
        function mapMessage(jsonObject) {
            console.log("STEP: START MAPPING...");
            return pipe(Message.decode(jsonObject), E.mapLeft((tErrors) => {
                console.log("MAP ERROR: ", tErrors);
                return new Error("MODEL MAPPING ERROR");
            }), TE.fromEither);
        }
        return TE.chain(mapMessage)(jsonObjectTE);
    }
    resolveMessage(messageTE) {
        return TE.getOrElse((error) => {
            console.log("STEP: RESOLVING");
            return T.of({ message: error.message, status: "Down" });
        })(messageTE);
    }
    assignMessage(message) {
        this.signal = message;
    }
    connectedCallback() {
        super.connectedCallback();
        console.log("****************** connectedCallback BEGIN");
        pipe(`${Config.appUrl}/ping`, this.fetchMessage, this.getJsonObject, this.extractMessage, this.resolveMessage)().then(msg => this.assignMessage(msg));
    }
};
__decorate([
    property({ type: Object })
], TjFrontendApp.prototype, "signal", void 0);
TjFrontendApp = __decorate([
    customElement("tj-frontend-app")
], TjFrontendApp);
