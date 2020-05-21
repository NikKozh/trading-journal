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
                // TODO: не забыть раскомментить это обратно
                /*return response.ok ?
                    response.json() :
                    // TODO: в случае BadRequest тоже ловить Json, только свою кастомную модель ошибки
                    Promise.reject(
                        `SERVER PROBLEM. Response status:
                        ${response.status} ${response.statusText} for URL ${response.url}`
                    )*/
                return Promise.resolve({message: "OK", status: "UP", code: 123})
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
                        const fieldType = String(typeof object[field])
                        return `${field}: ${fieldType}`
                    }).join(", ")
                }

                const jsonExcessFields = jsonObjectFields.diff(modelFields) as Array<keyof J>
                const modelMissingFields = modelFields.diff(jsonObjectFields) as Array<keyof T>

                const formattedJsonExcessFields = formatErrorFields(jsonObject, jsonExcessFields)
                const formattedModelMissingFields = formatErrorFields(modelFakeObject, modelMissingFields)

                const jsonExcessFieldsMessage =
                    jsonExcessFields.length > 0 ?
                        O.some(`JSON excess fields: {${formattedJsonExcessFields}}`) :
                        O.none

                const messageMissingFieldsMessage =
                    modelMissingFields.length > 0 ?
                        O.some(`Model missing fields: {${formattedModelMissingFields}}`) :
                        O.none

                return new Error(`
                    fields are not match. Details: 
                    ${A.compact([jsonExcessFieldsMessage, messageMissingFieldsMessage]).join(" ")}
                `)
            }

            function isFieldSetsEqual(): boolean {
                function isFieldsCountEqual(): boolean {
                    return jsonObjectFields.length === modelFields.length
                }

                function isFieldsNamesEqual(): boolean {
                    return pipe(
                        A.zip(jsonObjectFields.sort(), modelFields.sort()),
                        A.findFirst(([jsonField, messageField]) => jsonField !== messageField),
                        O.isNone
                    )
                }

                return isFieldsCountEqual() && isFieldsNamesEqual()
            }

            // TODO: добавить ещё одну финальную проверку на то, что поля в JSON идут в том же порядке (важно для маппинга)

            return isFieldSetsEqual() ?
                E.right(jsonObject) :
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

            // TODO: в некоторых случаях в errors оказывается несколько абсолютно одинаковых ошибок
            //  (скорее всего тогда, когда декодер не узнаёт тип) - надо научиться распознавать такую ситуацию
            //  (например, глубоким сравнением контекста) и отсекать лишнее
            return errors.map(getMessage)
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