import {TaskEither} from "fp-ts/es6/TaskEither"
import * as TE from "fp-ts/es6/TaskEither"
import * as T from "fp-ts/es6/Task"
import {Task} from "fp-ts/es6/Task"
import {flow} from "fp-ts/es6/function"
import {Either} from "fp-ts/es6/Either"
import * as O from "fp-ts/es6/Option"
import {Option} from "fp-ts/es6/Option"
import * as A from "fp-ts/es6/Array"
import {pipe} from "fp-ts/es6/pipeable"
import * as E from "fp-ts/es6/Either"
import * as t from "io-ts"
import {Type} from "@orchestrator/gen-io-ts/lib/types"
import {genIoType} from "@orchestrator/gen-io-ts"
import {eqString} from "fp-ts/es6/Eq"
import {ordString} from "fp-ts/es6/Ord"
import DetailedError from "../models/DetailedError"
import EventBus from "./EventBus"
import {isFullPermissions} from "./Helper"

// TODO: файл раздувается. Раскидать на модули поменьше (для моделей; простые запросы; запросы, не требующие ответа)

// TODO: Подумать про вывод тела ответа запроса - насколько нужно и как сделать
//  (там сейчас проблема, что тело достаётся через промис, который заблокирован другим потоком)


// TODO: опять же, очень тупо, особенно без HTTPS, но потом перепишу на JWT
function addAuthHeader(currentFetchProps?: RequestInit): RequestInit {
    let authHeader = { "For-Guest": String(!isFullPermissions()) }

    if (currentFetchProps) {
        currentFetchProps.headers = { ...currentFetchProps.headers, ...authHeader }
        return currentFetchProps
    } else {
        return {
            headers: {
                ...authHeader
            }
        }
    }
}

export function getResponse(fetchProps?: RequestInit) {
    return function (url: string): TaskEither<DetailedError, Response> {
        return TE.tryCatch(
            () => {
                console.log("STEP: START FETCHING...")
                return fetch(url, addAuthHeader(fetchProps))
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
}

function resolveErrorModelJson(response: Response, responseJsonPromise: Promise<Object>): Task<DetailedError> {
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

    const extractionOrServerError =
        pipe(errorJsonObjectTE,
            extractModel(DetailedError, false),
            TE.chain((extractionErrorModelTEArray: TaskEither<DetailedError, DetailedError>[]) =>
                extractionErrorModelTEArray[0] // по-идее, здесь всегда будет 1 элемент
            )
        )

    // TODO: где-то здесь добавить приписку в начале к details, что сервер отдал некорректную модель ошибки
    return TE.getOrElse(T.of)(extractionOrServerError)
}

function otherServerResponseError(response: Response): DetailedError {
    console.log("Response body: ", response)
    return new DetailedError(
        "RESPONSE ERROR",
        `Response status "${response.status} ${response.statusText}" for URL ${response.url}`,
        `Некорректный ответ от сервера. Тело ответа см. в консоли.`
    )
}

export function getPossibleErrorModel(responseTE: TaskEither<DetailedError, Response>): Task<Option<DetailedError>> {
    return TE.fold<DetailedError, Response, Option<DetailedError>>(
        (error: DetailedError) => T.of(O.some(error)),
        (response: Response) => {
            switch (response.status) {
                case 200: // OK
                    return T.of(O.none)

                case 400: // BadRequest
                    const responseJsonPromise: Promise<Object> = response.json()
                    return T.map((error: DetailedError) => O.some(error))
                                (resolveErrorModelJson(response, responseJsonPromise))

                // TODO: добавить отдельную обработку для 404 (т.к. частый кейс)

                default: // Other
                    return T.of(O.some(otherServerResponseError(response)))
            }
    })(responseTE)
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

        switch (response.status) {
            case 200: // OK
                return resolveModelJson()

            case 400: // BadRequest
                return TE.leftTask(resolveErrorModelJson(response, responseJsonPromise))

            // TODO: добавить отдельную обработку для 404 (т.к. частый кейс)

            default: // Other
                return TE.left(otherServerResponseError(response))
        }
    })(responseTE)
}

// TODO: Подумать, насколько возможно выводить красивые ошибки для вложенных сущностей
//  (сейчас декодер с ними в целом справляется, но всякие методы для проверки равенства полей
//  и вывод путей до косячных значений - нет)
export function extractModel<M>(ModelType: Type<M>, expectArray: boolean) {
    return function (jsonObjectTE: TaskEither<DetailedError, Object>): TaskEither<DetailedError, TaskEither<DetailedError, M>[]> {
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

        function checkArrayInObject(jsonArrayOrObject: Object): TaskEither<DetailedError, Object[]> {
            if (expectArray) {
                if (Array.isArray(jsonArrayOrObject)) {
                    return TE.right(jsonArrayOrObject)
                } else {
                    console.log("Single object from server: ", jsonArrayOrObject)
                    // TODO: снова пустой caption
                    return TE.left(new DetailedError(
                        "MODEL MAPPING PROBLEM",
                        "Expect array from server, got single object.",
                        "For object structure output go to console."
                    ))
                }
            } else {
                if (Array.isArray(jsonArrayOrObject)) {
                    console.log("Array from server: ", jsonArrayOrObject)
                    // TODO: снова пустой caption
                    return TE.left(new DetailedError(
                        "MODEL MAPPING PROBLEM",
                        "Expect single object from server, got array.",
                        "For array output go to console."
                    ))
                } else {
                    return TE.right([jsonArrayOrObject])
                }
            }
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

        return pipe(jsonObjectTE,
            TE.chain(checkArrayInObject),
            TE.map(A.map(mapModel))
        )
    }
}

export function resolveModel<M>(onSuccess: (model: M) => void, onFailure: (error: DetailedError) => void) {
    return function (modelTE: TaskEither<DetailedError, TaskEither<DetailedError, M>[]>) {
        console.log("STEP: RESOLVING")
        TE.fold(
            (err: DetailedError) => T.of(onFailure(err)),
            (modelArray: TaskEither<DetailedError, M>[]) =>
                /* На самом деле здесь должен  быть только один объект в массиве, это гарантируется проверками
                 * на предыдущих этапах, но на всякий случай, если функцию использовали отдельно от общего алгоритма,
                 * здесь доп. проверка */
                (modelArray.length === 1)
                    ? TE.fold(
                        (err2: DetailedError) => T.of(onFailure(err2)),
                        (model: M) => T.of(onSuccess(model))
                    )(modelArray[0])
                    : (() => {
                        console.log("Models array: ", modelArray)
                        return T.of(onFailure(
                            new DetailedError(
                                "MODEL RESOLVING ERROR",
                                "Expect single model after mapping, got array of models.",
                                "For array output go to console."
                            )
                        ))
                    })()
        )(modelTE)()
    }
}

export function resolveModelArray<M>(onSuccess: (models: M[]) => void, onFailure: (error: DetailedError) => void) {
    return function (modelTE: TaskEither<DetailedError, TaskEither<DetailedError, M>[]>) {
        console.log("STEP: RESOLVING")
        TE.fold(
            (err: DetailedError) => T.of(onFailure(err)),
            (modelArray: TaskEither<DetailedError, M>[]) =>
                pipe(modelArray,
                    A.reduce(
                        TE.right([] as M[]),
                        // TODO: сейчас это позволяет вывести только первую ошибку из массива. Возможно, поменять на
                        //  тип TaskEither<DetailedError[], M[]> и выводить потом все ошибки из массива?
                        (acc: TaskEither<DetailedError, M[]>, model: TaskEither<DetailedError, M>) =>
                            TE.fold(
                                (err: DetailedError) => T.of(E.left(err)),
                                (model: M) =>
                                    TE.map((accModelArray: M[]) =>
                                        [...accModelArray, model]
                                    )(acc)
                            )(model)
                    ),
                    TE.fold(
                        (err: DetailedError) => T.of(onFailure(err)),
                        (models: M[]) => T.of(onSuccess(models))
                    )
                )
        )(modelTE)()
    }
}

export function resolvePossibleErrorModel(onSuccess: () => void, onFailure: (error: DetailedError) => void) {
    return function (serverErrorTO: Task<Option<DetailedError>>) {
        T.map(O.fold(onSuccess, onFailure))(serverErrorTO)()
    }
}

export function resolveSimpleResponse<T>(onSuccess: (response: T) => void, onFailure: (error: DetailedError) => void) {
    return function (responseTE: TaskEither<DetailedError, any>) {
        TE.fold(
            (err: DetailedError) => T.of(onFailure(err)),
            (response: T) => T.of(onSuccess(response))
        )(responseTE)()
    }
}

export function defaultActionOnError(additionalAction?: (err: DetailedError) => void) {
    return function(error: DetailedError): void {
        console.log("ERROR: ", error)
        EventBus.$emit("error-occurred", error)
        if (additionalAction) {
            additionalAction(error)
        }
    }
}

export function fetchAndResolve<M>(url: string,
                                   ModelType: Type<M>,
                                   onSuccess: (model: M) => void,
                                   onFailure: (err: DetailedError) => void = defaultActionOnError(),
                                   fetchProps?: RequestInit): void {
    pipe(url,
        getResponse(fetchProps),
        getJsonObject,
        extractModel(ModelType, false),
        resolveModel(onSuccess, onFailure)
    )
}

export function fetchAndResolveArray<M>(url: string,
                                        ModelType: Type<M>,
                                        onSuccess: (models: M[]) => void,
                                        onFailure: (err: DetailedError) => void = defaultActionOnError(),
                                        fetchProps?: RequestInit): void {
    pipe(url,
        getResponse(fetchProps),
        getJsonObject,
        extractModel(ModelType, true),
        resolveModelArray(onSuccess, onFailure)
    )
}

export function submitWithRecovery(url: string,
                                   onSuccess: () => void,
                                   onFailure: (err: DetailedError) => void = defaultActionOnError(),
                                   fetchProps?: RequestInit): void {
    pipe(url,
        getResponse(fetchProps),
        getPossibleErrorModel,
        resolvePossibleErrorModel(onSuccess, onFailure)
    )
}

export function simpleRequest<T>(url: string,
                                 onSuccess: (response: T) => void,
                                 onFailure: (err: DetailedError) => void = defaultActionOnError(),
                                 fetchProps?: RequestInit): void {
    pipe(url,
        getResponse(fetchProps),
        getJsonObject,
        resolveSimpleResponse(onSuccess, onFailure)
    )
}