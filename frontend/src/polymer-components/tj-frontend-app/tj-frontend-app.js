var __decorate = (this && this.__decorate) || function (decorators, target, key, desc) {
    var c = arguments.length, r = c < 3 ? target : desc === null ? desc = Object.getOwnPropertyDescriptor(target, key) : desc, d;
    if (typeof Reflect === "object" && typeof Reflect.decorate === "function") r = Reflect.decorate(decorators, target, key, desc);
    else for (var i = decorators.length - 1; i >= 0; i--) if (d = decorators[i]) r = (c < 3 ? d(r) : c > 3 ? d(target, key, r) : d(target, key)) || r;
    return c > 3 && r && Object.defineProperty(target, key, r), r;
};
import { LitElement, html, customElement, property, query } from "lit-element";
import * as t from "io-ts";
import * as TE from "fp-ts/es6/TaskEither";
import * as E from "fp-ts/es6/Either";
import { pipe } from "fp-ts/es6/pipeable";
import { flow } from "fp-ts/es6/function";
import Routes from "../../conf/Routes";
import * as O from "fp-ts/es6/Option";
import * as A from "fp-ts/es6/Array";
import "../../utils/arrayExpansion";
import "mwc-app-dialog";
const Message = t.type({
    ["message"]: t.string,
    ["status"]: t.string,
    ["code"]: t.number
}, "Message");
let TjFrontendApp = class TjFrontendApp extends LitElement {
    constructor() {
        super();
        this.signal = { message: "No message provided yet", status: "N/A", code: 0 };
        this.errorCaption = "";
        this.errorText = "";
        this.showError = false;
        console.log("****************** connectedCallback BEGIN");
        const outerContext = this;
        pipe(Routes.pingMessage, this.fetchMessage, this.getJsonObject, this.extractMessage)().then(msgE => this.resolveMessage(msgE, outerContext));
    }
    render() {
        return html `
            <style>
                :host {
                    display: block;
                }
                #error-alert {
                    --mdc-theme-surface: #ffe0e0
                }
            </style>
            <mwc-app-dialog id="error-alert"></mwc-app-dialog> 
            <h2>Message: ${this.signal.message}</h2>
            <h2>Status: ${this.signal.status}</h2>
        `;
    }
    // TODO: улучшить обработку ошибок - вместо flow(String, Error) сделать что-нибудь поумнее
    fetchMessage(url) {
        return TE.tryCatch(() => {
            console.log("STEP: START FETCHING...");
            return fetch(url);
        }, flow(String, Error));
    }
    // TODO: улучшить обработку ошибок - вместо flow(String, Error) сделать что-нибудь поумнее
    getJsonObject(responseTE) {
        return TE.chain((response) => {
            return TE.tryCatch(() => {
                console.log("STEP: GETTING JSON...");
                /*return response.ok ?
                    response.json() :
                    Promise.reject(
                        `SERVER PROBLEM. Response status:
                        ${response.status} ${response.statusText} for URL ${response.url}`
                    )*/
                return Promise.resolve({ code: 123, status: "23", message: 123, anotherField: "123" });
            }, flow(String, Error));
        })(responseTE);
    }
    extractMessage(jsonObjectTE) {
        /* Сначала проверяем, что нет лишних полей
         * <br>
         * TODO: посмотреть свежим взглядом, мб можно ещё что-нибудь сократить\оптимизировать\переписать
         *  + вынести все функции, которые могут быть полезные где-то ещё
         *  +
         * */
        function checkFieldSetsEquality(jsonObject) {
            const jsonObjectFields = Object.getOwnPropertyNames(jsonObject);
            const messageFields = Object.getOwnPropertyNames(Message.props);
            function resolveFailedFields() {
                function getJsonType(value) {
                    return String(typeof jsonObject[value]);
                }
                function getMessageType(value) {
                    return Message.props[value].name;
                }
                function formatErrorFields(errorFields, getType) {
                    return errorFields.map(value => {
                        return `${value}: ${getType(value)}`;
                    }).join(", ");
                }
                const jsonExcessFields = jsonObjectFields.diff(messageFields);
                const messageMissingFields = messageFields.diff(jsonObjectFields);
                const formattedJsonExcessFields = formatErrorFields(jsonExcessFields, getJsonType);
                const formattedMessageMissingFields = formatErrorFields(messageMissingFields, getMessageType);
                const jsonExcessFieldsMessage = jsonExcessFields.length > 0 ?
                    O.some(`JSON excess fields: {${formattedJsonExcessFields}}`) :
                    O.none;
                const messageMissingFieldsMessage = messageMissingFields.length > 0 ?
                    O.some(`Model missing fields: {${formattedMessageMissingFields}}`) :
                    O.none;
                return new Error(`
                    fields are not match. Details: 
                    ${A.compact([jsonExcessFieldsMessage, messageMissingFieldsMessage]).join("; ")}
                `);
            }
            function isFieldSetsEqual() {
                function isFieldsCountEqual() {
                    return jsonObjectFields.length === messageFields.length;
                }
                function isFieldsNamesEqual() {
                    return pipe(A.zip(jsonObjectFields.sort(), messageFields.sort()), A.findFirst(([jsonField, messageField]) => jsonField !== messageField), O.isNone);
                }
                return isFieldsCountEqual() && isFieldsNamesEqual();
            }
            return isFieldSetsEqual() ?
                E.right(jsonObject) :
                E.left(resolveFailedFields());
        }
        function formatMapErrors(errors) {
            function getContextPath(context) {
                return `${Message.name}.${context[1].key} (${context[1].type.name})`;
            }
            function getMessage(error) {
                return `Invalid value ${JSON.stringify(error.value)} (${typeof error.value})
                        supplied to ${getContextPath(error.context)}`;
            }
            return errors.map(getMessage);
        }
        function mapMessage(jsonObject) {
            function simplifyValidationErrors(tErrors) {
                const formattedErrors = formatMapErrors(tErrors).join("; ");
                const errorsMessage = `types are not compatible. Details: ${formattedErrors}`;
                console.log("MAP ERROR: ", errorsMessage);
                return new Error(errorsMessage);
            }
            console.log("STEP: START MAPPING...");
            return pipe(jsonObject, checkFieldSetsEquality, E.chain(objectWithCheckedFieldSets => E.mapLeft(simplifyValidationErrors)(Message.decode(objectWithCheckedFieldSets))), E.mapLeft(errors => new Error(`MODEL MAPPING PROBLEM. ${errors}`)), TE.fromEither);
        }
        return TE.chain(mapMessage)(jsonObjectTE);
    }
    // TODO: дело происходит в промисе, поэтому требуется _this. Наверняка это можно сделать как-то получше
    resolveMessage(messageE, _this) {
        console.log("STEP: RESOLVING, _this: ", _this);
        E.fold((error) => {
            this.errorAlert.notice("Заголовок ошибки", error.message);
            _this.signal = { message: "error", status: "down", code: -1 };
        }, (message) => this.signal = message)(messageE);
    }
};
__decorate([
    property()
], TjFrontendApp.prototype, "signal", void 0);
__decorate([
    property()
], TjFrontendApp.prototype, "errorCaption", void 0);
__decorate([
    property()
], TjFrontendApp.prototype, "errorText", void 0);
__decorate([
    property()
], TjFrontendApp.prototype, "showError", void 0);
__decorate([
    query("#error-alert")
], TjFrontendApp.prototype, "errorAlert", void 0);
TjFrontendApp = __decorate([
    customElement("tj-frontend-app")
], TjFrontendApp);
