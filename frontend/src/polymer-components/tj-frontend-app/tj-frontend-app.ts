import {LitElement, html, customElement, property, TemplateResult, query, PropertyValues} from "lit-element";
import * as t from "io-ts";
import * as TE from "fp-ts/es6/TaskEither";
import {TaskEither} from "fp-ts/es6/TaskEither";
import * as E from "fp-ts/es6/Either";
import {Either} from "fp-ts/es6/Either";
import * as T from "fp-ts/es6/Task";
import {Task} from "fp-ts/es6/Task";
import {pipe} from "fp-ts/es6/pipeable";
import {flow} from "fp-ts/es6/function";
import Routes from "../../conf/Routes";
import * as O from "fp-ts/es6/Option";
import * as A from "fp-ts/es6/Array"
import * as io from "fp-ts/es6/IO"
import {IO} from "fp-ts/es6/IO"
import "../../utils/arrayExpansion"
import "mwc-app-dialog"
import {MwcAppDialog} from "mwc-app-dialog/MwcAppDialog";

const Message = t.type({
    ["message" as string]: t.string,
    ["status" as string]: t.string,
    ["code" as string]: t.number
}, "Message");

type Message = t.TypeOf<typeof Message>

@customElement("tj-frontend-app")
class TjFrontendApp extends LitElement {
    @property()
    signal: Message = { message: "No message provided yet", status: "N/A", code: 0 }

    @property()
    errorCaption: string = ""

    @property()
    errorText: string = ""

    @property()
    showError: boolean = false

    @query("#error-alert")
    errorAlert!: MwcAppDialog;

    constructor() {
        super();

        console.log("****************** connectedCallback BEGIN")

        const outerContext: TjFrontendApp = this;

        pipe(Routes.pingMessage,
            this.fetchMessage,
            this.getJsonObject,
            this.extractMessage
        )().then(msgE =>
            this.resolveMessage(msgE, outerContext)
        )
    }

    render(): TemplateResult {
        return html`
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
        `
    }

    // TODO: улучшить обработку ошибок - вместо flow(String, Error) сделать что-нибудь поумнее
    private fetchMessage(url: string): TaskEither<Error, Response> {
        return TE.tryCatch(
            () => {
                console.log("STEP: START FETCHING...");
                return fetch(url)
            },
            flow(String, Error)
        )
    }

    // TODO: улучшить обработку ошибок - вместо flow(String, Error) сделать что-нибудь поумнее
    private getJsonObject(responseTE: TaskEither<Error, Response>): TaskEither<Error, Object> {
        return TE.chain((response: Response) => {
            return TE.tryCatch(
                () => {
                    console.log("STEP: GETTING JSON...")
                    /*return response.ok ?
                        response.json() :
                        Promise.reject(
                            `SERVER PROBLEM. Response status:
                            ${response.status} ${response.statusText} for URL ${response.url}`
                        )*/
                    return Promise.resolve({code: 123, status: "23", message: 123, anotherField: "123"})
                },
                flow(String, Error)
            )
        })(responseTE)
    }

    private extractMessage(jsonObjectTE: TaskEither<Error, Object>): TaskEither<Error, Message> {
        /* Сначала проверяем, что нет лишних полей
         * <br>
         * TODO: посмотреть свежим взглядом, мб можно ещё что-нибудь сократить\оптимизировать\переписать
         *  + вынести все функции, которые могут быть полезные где-то ещё
         *  +
         * */
        function checkFieldSetsEquality<J extends object>(jsonObject: J): Either<Error, Object> {
            const jsonObjectFields = Object.getOwnPropertyNames(jsonObject)
            const messageFields = Object.getOwnPropertyNames(Message.props)

            function resolveFailedFields(): Error {
                function getJsonType(value: keyof J): string {
                    return String(typeof jsonObject[value])
                }

                function getMessageType(value: string | number): string {
                    return Message.props[value].name
                }

                function formatErrorFields<T extends object>(errorFields: Array<keyof T>,
                                                             getType: (v: keyof T) => string): string {
                    return errorFields.map(value => {
                        return `${value}: ${getType(value)}`
                    }).join(", ")
                }

                const jsonExcessFields = jsonObjectFields.diff(messageFields) as Array<keyof J>
                const messageMissingFields = messageFields.diff(jsonObjectFields)

                const formattedJsonExcessFields = formatErrorFields(jsonExcessFields, getJsonType)
                const formattedMessageMissingFields = formatErrorFields(messageMissingFields, getMessageType)

                const jsonExcessFieldsMessage =
                    jsonExcessFields.length > 0 ?
                        O.some(`JSON excess fields: {${formattedJsonExcessFields}}`) :
                        O.none

                const messageMissingFieldsMessage =
                    messageMissingFields.length > 0 ?
                        O.some(`Model missing fields: {${formattedMessageMissingFields}}`) :
                        O.none

                return new Error(`
                    fields are not match. Details: 
                    ${A.compact([jsonExcessFieldsMessage, messageMissingFieldsMessage]).join("; ")}
                `)
            }

            function isFieldSetsEqual(): boolean {
                function isFieldsCountEqual(): boolean {
                    return jsonObjectFields.length === messageFields.length
                }

                function isFieldsNamesEqual(): boolean {
                    return pipe(
                        A.zip(jsonObjectFields.sort(), messageFields.sort()),
                        A.findFirst(([jsonField, messageField]) => jsonField !== messageField),
                        O.isNone
                    )
                }

                return isFieldsCountEqual() && isFieldsNamesEqual()
            }

            return isFieldSetsEqual() ?
                E.right(jsonObject) :
                E.left(resolveFailedFields())
        }

        function formatMapErrors(errors: t.Errors): string[] {
            function getContextPath(context: t.Context): string {
                return `${Message.name}.${context[1].key} (${context[1].type.name})`
            }

            function getMessage(error: t.ValidationError): string {
                return `Invalid value ${JSON.stringify(error.value)} (${typeof error.value})
                        supplied to ${getContextPath(error.context)}`
            }

            return errors.map(getMessage)
        }

        function mapMessage(jsonObject: Object): TaskEither<Error, Message> {
            function simplifyValidationErrors(tErrors: t.Errors): Error {
                const formattedErrors = formatMapErrors(tErrors).join("; ");
                const errorsMessage = `types are not compatible. Details: ${formattedErrors}`

                console.log("MAP ERROR: ", errorsMessage)
                return new Error(errorsMessage)
            }

            console.log("STEP: START MAPPING...");

            return pipe(jsonObject,
                checkFieldSetsEquality,
                E.chain(objectWithCheckedFieldSets =>
                    E.mapLeft(simplifyValidationErrors)(Message.decode(objectWithCheckedFieldSets))
                ),
                E.mapLeft(errors => new Error(`MODEL MAPPING PROBLEM. ${errors}`)),
                TE.fromEither
            )
        }

        return TE.chain(mapMessage)(jsonObjectTE)
    }

    // TODO: дело происходит в промисе, поэтому требуется _this. Наверняка это можно сделать как-то получше
    private resolveMessage(messageE: Either<Error, Message>, _this: TjFrontendApp): void {
        console.log("STEP: RESOLVING, _this: ", _this);

        E.fold(
            (error: Error) => {
                this.errorAlert.notice("Заголовок ошибки", error.message);

                _this.signal = {message: "error", status: "down", code: -1};
            },
            (message: Message) => this.signal = message
        )(messageE)
    }
}