import {html, PolymerElement} from "@polymer/polymer/polymer-element";
import {customElement, property} from "@polymer/decorators/lib/decorators";
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
import "../../utils/arrayExpansion"

const Message = t.type({
    ["message" as string]: t.string,
    ["status" as string]: t.string,
    ["code" as string]: t.number
}, "Message");

type Message = t.TypeOf<typeof Message>

@customElement("tj-frontend-app")
class TjFrontendApp extends PolymerElement {
    @property({type: Object})
    signal: Message = { message: "No message provided yet", status: "N/A", code: 0 }

    static get template(): HTMLTemplateElement {
        return html`
            <style>
                :host {
                    display: block;
                }
            </style>
            <h2>Message: [[signal.message]]</h2>
            <h2>Status: [[signal.status]]</h2>
        `;
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

    // TODO: вместо резолва в конечную модель научиться выводить модалки с ошибкой
    private resolveMessage(messageTE: TaskEither<Error, Message>): Task<Message> {
        return TE.getOrElse<Error, Message>((error: Error) => {
            console.log("STEP: RESOLVING")
            return T.of({message: error.message, status: "Down", code: -1})
        })(messageTE)
    }

    private assignMessage(message: Message): void {
        this.signal = message;
    }

    connectedCallback(): void {
        super.connectedCallback();

        console.log("****************** connectedCallback BEGIN")

        pipe(Routes.pingMessage,
            this.fetchMessage,
            this.getJsonObject,
            this.extractMessage,
            this.resolveMessage
        )().then(msg => this.assignMessage(msg))
    }
}