export function varToString(varObj: any): string {
    return Object.keys(varObj)[0]
}

export const getPropertyString = <T extends object>(object: T, property: keyof T) => {
    return String(object[property])
};