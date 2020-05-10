import {html, PolymerElement} from "@polymer/polymer/polymer-element";
import {customElement, property} from "@polymer/decorators/lib/decorators";
import * as t from "io-ts";
import * as TE from "fp-ts/es6/TaskEither";
import {TaskEither} from "fp-ts/es6/TaskEither";
import * as E from "fp-ts/es6/Either";
import * as T from "fp-ts/es6/Task";
import {Task} from "fp-ts/es6/Task";
import Config from '../conf/Config';
import {pipe} from "fp-ts/es6/pipeable";
import {flow} from "fp-ts/es6/function";

const Message = t.type({
    message: t.string,
    status: t.string
});

type Message = t.TypeOf<typeof Message>

@customElement("tj-frontend-app")
class TjFrontendApp extends PolymerElement {
    @property({type: Object})
    signal: Message = { message: "No message provided yet", status: "N/A" }

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

    private fetchMessage(url: string): TaskEither<Error, Response> {
        return TE.tryCatch(
            () => {
                console.log("STEP: START FETCHING...");
                return fetch(url)
            },
            flow(String, Error)
        )
    }

    private getJsonObject(responseTE: TaskEither<Error, Response>): TaskEither<Error, Object> {
        return TE.chain((response: Response) => {
            return TE.tryCatch(
                () => {
                    console.log("STEP: GETTING JSON...")
                    return response.json()
                },
                flow(String, Error)
            )
        })(responseTE)
    }

    private extractMessage(jsonObjectTE: TaskEither<Error, Object>): TaskEither<Error, Message> {
        /* Сначала проверяем, что нет лишних полей */
        function validateMessage() {
            // not implemented yet
        }
        function mapMessage(jsonObject: Object): TaskEither<Error, Message> {
            console.log("STEP: START MAPPING...");
            return pipe(Message.decode(jsonObject),
                E.mapLeft((tErrors: t.Errors) => {
                    console.log("MAP ERROR: ", tErrors)
                    return new Error("MODEL MAPPING ERROR")
                }),
                TE.fromEither
            )
        }

        return TE.chain(mapMessage)(jsonObjectTE)
    }

    private resolveMessage(messageTE: TaskEither<Error, Message>): Task<Message> {
        return TE.getOrElse<Error, Message>((error: Error) => {
            console.log("STEP: RESOLVING")
            return T.of({message: error.message, status: "Down"})
        })(messageTE)
    }

    private assignMessage(message: Message): void {
        this.signal = message;
    }

    connectedCallback(): void {
        super.connectedCallback();

        console.log("****************** connectedCallback BEGIN")

        pipe(`${Config.appUrl}/ping`,
            this.fetchMessage,
            this.getJsonObject,
            this.extractMessage,
            this.resolveMessage
        )().then(msg => this.assignMessage(msg))
    }
}