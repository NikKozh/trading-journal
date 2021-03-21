import {Option} from "fp-ts/es6/Option";
import * as A from "fp-ts/es6/Array";
import Vue from "vue"

function jsonReplacer<T>(key: string, value: T | Option<T>): T | string | null {
    if ((typeof value === 'object') && value && ("_tag" in value)) {
        if (value._tag === 'Some') {
            return value.value
        } else {
            return null
        }
    } else if (Array.isArray(value) && (value.length > 0) && (typeof value[0] === 'string')) {
        // срезаем "data:image/png;base64," и оставляем только сам BASE64
        // TODO: схлопываем массив до строки, чтобы соответствовать временной модели с бэкенда (вернуть потом как было)
        return A.map((el: string) => el.startsWith("data:image/png;base64,") ? el.slice(22) : el)(value).join(";")
    } else if (Array.isArray(value) && (value.length === 0)) {
        return ""
    }
    return value
}

export function smartJsonStringify<T>(obj: T): string {
    return JSON.stringify(obj, jsonReplacer)
}

export function isFullPermissions(): boolean {
    return Vue.cookies.isKey("user_password")
}

export function setAuthCookie(pass: string): void {
    Vue.cookies.set("user_password", pass, { expires: Infinity })
}

export function removeAuthCookie(): void {
    if (isFullPermissions()) {
        Vue.cookies.remove("user_password")
    }
}