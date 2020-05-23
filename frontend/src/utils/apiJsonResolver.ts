import {TaskEither} from "fp-ts/es6/TaskEither"
import * as TE from "fp-ts/es6/TaskEither"
import * as Task from "fp-ts/es6/Task"
import {flow} from "fp-ts/es6/function"
import {Either} from "fp-ts/es6/Either"
import * as O from "fp-ts/es6/Option"
import * as A from "fp-ts/es6/Array"
import {pipe} from "fp-ts/es6/pipeable"
import * as E from "fp-ts/es6/Either"
import * as t from "io-ts"
import {Type} from "@orchestrator/gen-io-ts/lib/types"
import {genIoType} from "@orchestrator/gen-io-ts"
import {eqString} from "fp-ts/es6/Eq";
import {ordString} from "fp-ts/es6/Ord";

// TODO: переделать все Error на свою кастомную модель

// TODO: улучшить обработку ошибок - вместо flow(String, Error) сделать что-нибудь поумнее
export function getResponse(url: string): TaskEither<Error, Response> {
    return TE.tryCatch(
        () => {
            console.log("STEP: START FETCHING...")
            return fetch(url)
        },
        flow(String, Error)
    )
}

// TODO: улучшить обработку ошибок - вместо flow(String, Error) сделать что-нибудь поумнее
export function getJsonObject(responseTE: TaskEither<Error, Response>): TaskEither<Error, Object> {
    return TE.chain((response: Response) => {
        return TE.tryCatch(
            () => {
                console.log("STEP: GETTING JSON...")
                return response.ok ?
                    response.json() :
                    // TODO: в случае BadRequest тоже ловить Json, только свою кастомную модель ошибки
                    Promise.reject(
                        `SERVER PROBLEM. Response status:
                        ${response.status} ${response.statusText} for URL ${response.url}`
                    )
            },
            flow(String, Error)
        )
    })(responseTE)
}

export function extractModel<T>(ModelType: Type<T>) {
    return function (jsonObjectTE: TaskEither<Error, Object>): TaskEither<Error, T> {
        const ModelCodec = genIoType(ModelType)
        const modelFakeObject = new ModelType() // небольшой хак, чтобы достать из модели список полей и их типы

        function checkFieldSetsEquality<J extends object>(jsonObject: J): Either<Error, Object> {
            const jsonObjectFields = Object.getOwnPropertyNames(jsonObject)
            const modelFields = Object.getOwnPropertyNames(modelFakeObject)

            function resolveFailedFields(): Error {
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
                const modelMissingFields = A.difference(eqString)(modelFields, jsonObjectFields) as Array<keyof T>

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

                return new Error(`
                    fields are not match. Details: 
                    ${A.compact([jsonExcessFieldsMessage, messageMissingFieldsMessage]).join(" ")}
                `)
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

            function checkFieldsOrder(): Either<Error, Object> {
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
                    E.left(Error("fields are match, but order is wrong."))
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

        function mapModel(jsonObject: Object): TaskEither<Error, T> {
            function simplifyValidationErrors(tErrors: t.Errors): Error {
                const formattedErrors = formatMapErrors(tErrors).join(" ")
                const errorsMessage = `types are not compatible. Details: ${formattedErrors}`

                console.log("MAP ERROR: ", errorsMessage)
                return new Error(errorsMessage)
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
                E.mapLeft(errors => new Error(`MODEL MAPPING PROBLEM. ${errors}`)),
                TE.fromEither
            )
        }

        return TE.chain(mapModel)(jsonObjectTE)
    }
}

export function resolveModel<T>(onSuccess: (model: T) => void, onFailure: (error: Error) => void) {
    return function (modelTE: TaskEither<Error, T>) {
        console.log("STEP: RESOLVING")
        TE.fold(
            (err: Error) => Task.of(onFailure(err)),
            (model: T) => Task.of(onSuccess(model))
        )(modelTE)()
    }
}

export function fetchAndResolve<T>(url: string,
                                   ModelType: Type<T>,
                                   onSuccess: (model: T) => void,
                                   onFailure: (err: Error) => void): void {
    pipe(url,
        getResponse,
        getJsonObject,
        extractModel(ModelType),
        resolveModel(onSuccess, onFailure)
    )
}