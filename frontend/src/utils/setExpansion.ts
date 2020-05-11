export {}

declare global {
    interface Set<T> {
        union(that: Set<T>): Set<T>
        intersect(that: Set<T>): Set<T>
        diff(that: Set<T>): Set<T>
    }
}

if (!Set.prototype.union) {
    Set.prototype.union = function<T>(this: Set<T>, that: Set<T>): Set<T> {
        return new Set([...this, ...that])
    }
}

if (!Set.prototype.intersect) {
    Set.prototype.intersect = function<T>(this: Set<T>, that: Set<T>): Set<T> {
        return new Set([...this].filter(x => that.has(x)))
    }
}

if (!Set.prototype.diff) {
    Set.prototype.diff = function<T>(this: Set<T>, that: Set<T>): Set<T> {
        return new Set([...this].filter(x => !that.has(x)))
    }
}