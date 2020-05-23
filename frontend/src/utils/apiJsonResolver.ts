import {TaskEither} from "fp-ts/es6/TaskEither"
import * as TE from "fp-ts/es6/TaskEither"
import * as T from "fp-ts/es6/Task"
import {Task} from "fp-ts/es6/Task"
import {flow} from "fp-ts/es6/function"
import {Either} from "fp-ts/es6/Either"
import * as O from "fp-ts/es6/Option"
import * as A from "fp-ts/es6/Array"
import {pipe} from "fp-ts/es6/pipeable"
import * as E from "fp-ts/es6/Either"
import * as t from "io-ts"
import {Type} from "@orchestrator/gen-io-ts/lib/types"
import {genIoType} from "@orchestrator/gen-io-ts"
import {eqString} from "fp-ts/es6/Eq"
import {ordString} from "fp-ts/es6/Ord"
import {DetailedError} from "../models/DetailedError"

// TODO: Подумать про вывод тела ответа запроса - насколько нужно и как сделать
//  (там сейчас проблема, что тело достаётся через промис, который заблокирован другим потоком)

export function getResponse(url: string): TaskEither<DetailedError, Response> {
    return TE.tryCatch(
        () => {
            console.log("STEP: START FETCHING...")
            return fetch(url)
        },
        flow(
            String,
            errorStr => new DetailedError(
                 "FETCH ERROR",
                 errorStr,
                 `Проблема с доступом к ресурсу по URL: ${url}`
             )
        )
    )
}

export function getJsonObject(responseTE: TaskEither<DetailedError, Response>): TaskEither<DetailedError, Object> {
    return TE.chain((response: Response) => {
        const responseJsonPromise: Promise<Object> = response.json()

        function resolveModelJson(): TaskEither<DetailedError, Object> {
            return TE.tryCatch(
                () => responseJsonPromise,
                flow(
                    String,
                    errStr => {
                        console.log("Response body: ", response.body)
                        return new DetailedError(
                            "JSON ERROR",
                            errStr,
                            `Сервер ответил "200 OK" при обращении к ${response.url},
                             но не удалось получить или распарсить JSON. Тело ответа см. в консоли.`
                        )
                    }
                )
            )
        }

        function resolveErrorModelJson(): Task<DetailedError> {
            const errorJsonObjectTE = 
                TE.tryCatch(
                    () => responseJsonPromise,
                    flow(
                        String,
                        errorStr => {
                            console.log("Response body: ", response.body)
                            return new DetailedError(
                                "SERVER PROBLEM",
                                errorStr,
                                `Сервер ответил "400 BadRequest" при обращении к ${response.url}, но получить или
                                 распарсить JSON с моделью ошибки не удалось. Тело ответа см. в консоли.`
                            )
                        }
                    )
                )

            const extractionOrServerError = extractModel(DetailedError)(errorJsonObjectTE)

            // TODO: где-то здесь добавить приписку в начале к details, что сервер отдал некорректную модель ошибки
            return TE.getOrElse(T.of)(extractionOrServerError)
        }

        function otherServerResponseError(): DetailedError {
            console.log("Response body: ", response)
            return new DetailedError(
                "RESPONSE ERROR",
                `Response status "${response.status} ${response.statusText}" for URL ${response.url}`,
                `Некорректный ответ от сервера. Тело ответа см. в консоли.`
            )
        }

        switch (response.status) {
            case 200: // OK
                return resolveModelJson()

            case 400: // BadRequest
                return TE.leftTask(resolveErrorModelJson())

            // TODO: добавить отдельную обработку для 404 (т.к. частый кейс)

            default: // Other
                return TE.left(otherServerResponseError())
        }
    })(responseTE)
}

export function extractModel<M>(ModelType: Type<M>) {
    return function (jsonObjectTE: TaskEither<DetailedError, Object>): TaskEither<DetailedError, M> {
        const ModelCodec = genIoType(ModelType)
        const modelFakeObject = new ModelType() // небольшой хак, чтобы достать из модели список полей и их типы

        function checkFieldSetsEquality<J extends object>(jsonObject: J): Either<DetailedError, Object> {
            const jsonObjectFields = Object.getOwnPropertyNames(jsonObject)
            const modelFields = Object.getOwnPropertyNames(modelFakeObject)

            function resolveFailedFields(): DetailedError {
                function formatErrorFields<U>(object: U, errorFields: Array<keyof U>): string {
                    return errorFields.map(field => {
                        // console.log("TYPE: ", ModelCodec.props[field].name)
                        // TODO: подумать над случаями, когда в модели сложный тип (например, Option<Number>)
                        //  в таких случаях fieldType === object, а значит теряется весь смысл этого вывода
                        //  (вот на стадии маппинга мы в целом умеем доставать тип через контекст типа t.Errors)
                        const fieldType = typeof object[field]
                        return `${field}: ${String(fieldType)}`
                    }).join(", ")
                }

                const jsonExcessFields = A.difference(eqString)(jsonObjectFields, modelFields) as Array<keyof J>
                const modelMissingFields = A.difference(eqString)(modelFields, jsonObjectFields) as Array<keyof M>

                const formattedJsonExcessFields = formatErrorFields(jsonObject, jsonExcessFields)
                const formattedModelMissingFields = formatErrorFields(modelFakeObject, modelMissingFields)

                const jsonExcessFieldsMessage =
                    jsonExcessFields.length > 0 ?
                        O.some(`JSON excess fields: {${formattedJsonExcessFields}}`) :
                        O.none

                const messageMissingFieldsMessage =
                    modelMissingFields.length > 0 ?
                        O.some(`Model missing fields: ${ModelCodec.name} { ${formattedModelMissingFields} }`) :
                        O.none

                // TODO: вот здесь получается вынуждены проставлять пустой caption, т.к. он ещё неизвестен.
                //  Некрасиво. Надо поправить (сделать его опциональным в модели или ещё как-то)
                return new DetailedError(
                    "",
                    "Fields are not match.",
                    A.compact([jsonExcessFieldsMessage, messageMissingFieldsMessage]).join(" ")
                )
            }

            function isFieldSetsEqual(): boolean {
                function isCountEqual(): boolean {
                    return jsonObjectFields.length === modelFields.length
                }

                function isNamesEqual(): boolean {
                    return pipe(
                        A.zip(A.sort(ordString)(jsonObjectFields), A.sort(ordString)(modelFields)),
                        A.findFirst(([jsonField, messageField]) => jsonField !== messageField),
                        O.isNone
                    )
                }

                return isCountEqual() && isNamesEqual()
            }

            function checkFieldsOrder(): Either<DetailedError, Object> {
                // TODO: forall в библиотеке fp-ts? (написать свою функцию-утилиту?)
                const isOrderRight =
                    pipe(A.zip(jsonObjectFields, modelFields),
                        A.dropLeftWhile(zipped => {
                            const [jsonField, modelField] = zipped
                            return jsonField === modelField
                        }),
                        A.isEmpty
                    )

                return isOrderRight ?
                    E.right(jsonObject) :
                    // TODO: здесь то же самое, пустой caption
                    E.left(new DetailedError("", "Fields are match, but order is wrong."))
            }

            return isFieldSetsEqual() ?
                checkFieldsOrder() :
                E.left(resolveFailedFields())
        }

        function formatMapErrors(errors: t.Errors): string[] {
            function getContextPath(context: t.Context): string {
                return `${ModelCodec.name}.${context[1].key} (${context[1].type.name})`
            }

            function getMessage(error: t.ValidationError): string {
                return `Invalid value ${JSON.stringify(error.value)} (${typeof error.value})
                        supplied to ${getContextPath(error.context)}`
            }

            return A.uniq(eqString)(errors.map(getMessage))
        }

        function mapModel(jsonObject: Object): TaskEither<DetailedError, M> {
            function simplifyValidationErrors(tErrors: t.Errors): DetailedError {
                const formattedErrors = formatMapErrors(tErrors).join(" ")

                console.log("MAP ERROR: ", formattedErrors)
                // TODO: снова пустой caption
                return new DetailedError("", "Types are not compatible.", formattedErrors)
            }

            console.log("STEP: START MAPPING...")

            return pipe(jsonObject,
                checkFieldSetsEquality,
                E.chain(objectWithCheckedFieldSets =>
                    E.bimap(
                        simplifyValidationErrors,
                        // () => Object.assign(ModelType.prototype, objectWithCheckedFieldSets)
                        () => new ModelType(...Object.values(objectWithCheckedFieldSets))
                    )(ModelCodec.decode(objectWithCheckedFieldSets))
                ),
                // TODO: добавить сюда или ещё куда-то pretty-вывод ВСЕХ полей Модели и полученного JSON
                E.mapLeft(error => {
                    // TODO: МУТАБЕЛЬНОСТЬ! Подумать, так ли это критично и, если да, то как сделать .copy как в Скале
                    error.caption = "MODEL MAPPING PROBLEM"
                    return error
                }),
                TE.fromEither
            )
        }

        return TE.chain(mapModel)(jsonObjectTE)
    }
}

export function resolveModel<M>(onSuccess: (model: M) => void, onFailure: (error: DetailedError) => void) {
    return function (modelTE: TaskEither<DetailedError, M>) {
        console.log("STEP: RESOLVING")
        TE.fold(
            (err: DetailedError) => T.of(onFailure(err)),
            (model: M) => T.of(onSuccess(model))
        )(modelTE)()
    }
}

export function fetchAndResolve<M>(url: string,
                                   ModelType: Type<M>,
                                   onSuccess: (model: M) => void,
                                   onFailure: (err: DetailedError) => void): void {
    pipe(url,
        getResponse,
        getJsonObject,
        extractModel(ModelType),
        resolveModel(onSuccess, onFailure)
    )
}