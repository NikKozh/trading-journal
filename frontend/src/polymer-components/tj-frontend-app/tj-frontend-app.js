var __decorate = (this && this.__decorate) || function (decorators, target, key, desc) {
    var c = arguments.length, r = c < 3 ? target : desc === null ? desc = Object.getOwnPropertyDescriptor(target, key) : desc, d;
    if (typeof Reflect === "object" && typeof Reflect.decorate === "function") r = Reflect.decorate(decorators, target, key, desc);
    else for (var i = decorators.length - 1; i >= 0; i--) if (d = decorators[i]) r = (c < 3 ? d(r) : c > 3 ? d(target, key, r) : d(target, key)) || r;
    return c > 3 && r && Object.defineProperty(target, key, r), r;
};
var __metadata = (this && this.__metadata) || function (k, v) {
    if (typeof Reflect === "object" && typeof Reflect.metadata === "function") return Reflect.metadata(k, v);
};
import { LitElement, html, customElement, property as litProperty, query } from "lit-element";
import Routes from "../../conf/Routes";
import "../../utils/arrayExpansion";
import "mwc-app-dialog";
import { MwcAppDialog } from "mwc-app-dialog/MwcAppDialog";
import { Property as CodecProperty } from "@orchestrator/gen-io-ts";
import { fetchAndResolve } from "../../utils/apiJsonResolver";
class Message {
    constructor(message, status, code) {
        this.message = message;
        this.status = status;
        this.code = code;
    }
}
__decorate([
    CodecProperty({ isRequired: true }),
    __metadata("design:type", String)
], Message.prototype, "message", void 0);
__decorate([
    CodecProperty({ isRequired: true }),
    __metadata("design:type", String)
], Message.prototype, "status", void 0);
__decorate([
    CodecProperty({ isRequired: true }),
    __metadata("design:type", Number)
], Message.prototype, "code", void 0);
let TjFrontendApp = class TjFrontendApp extends LitElement {
    constructor() {
        super();
        this.signal = new Message("No message provided yet", "N/A", 0);
        fetchAndResolve(Routes.pingMessage, Message, (message) => this.signal = message, (error) => {
            this.errorAlert.notice("Заголовок ошибки", error.message);
            this.signal = new Message("error", "down", -1);
        });
    }
    render() {
        return html `
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
        `;
    }
};
__decorate([
    litProperty(),
    __metadata("design:type", Message)
], TjFrontendApp.prototype, "signal", void 0);
__decorate([
    query("#error-alert"),
    __metadata("design:type", MwcAppDialog)
], TjFrontendApp.prototype, "errorAlert", void 0);
TjFrontendApp = __decorate([
    customElement("tj-frontend-app"),
    __metadata("design:paramtypes", [])
], TjFrontendApp);
