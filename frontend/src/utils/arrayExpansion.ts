export {}

declare global {
    interface Array<T> {
        union(that: Array<T>): Array<T>
        intersect(that: Array<T>): Array<T>
        diff(that: Array<T>): Array<T>
    }
}

if (!Array.prototype.union) {
    Array.prototype.union = function<T>(this: Array<T>, that: Array<T>): Array<T> {
        return [...this, ...that]
    }
}

if (!Array.prototype.intersect) {
    Array.prototype.intersect = function<T>(this: Array<T>, that: Array<T>): Array<T> {
        return this.filter(x => that.includes(x))
    }
}

if (!Array.prototype.diff) {
    Array.prototype.diff = function<T>(this: Array<T>, that: Array<T>): Array<T> {
        return this.filter(x => !that.includes(x))
    }
}