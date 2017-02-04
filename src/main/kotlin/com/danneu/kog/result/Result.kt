package com.danneu.kog.result

// A Result<A, B> represents a value that is either Success<A> or Failure<B>.
//
// Fork of https://github.com/kittinunf/Result which allows nullable Result.Success values.
//
// More info: https://github.com/kittinunf/Result/issues/18

inline fun <reified X> Result<*, *>.getAs() = when (this) {
    is Result.Success -> value as? X
    is Result.Failure -> error as? X
}

fun <V> Result<V, *>.success(f: (V) -> Unit) = fold(f, {})

fun <E : Exception> Result<*, E>.failure(f: (E) -> Unit) = fold({}, f)

infix fun <V, E : Exception> Result<V, E>.or(fallback: V) = when (this) {
    is Result.Success -> this
    else -> Result.Success<V, E>(fallback)
}

infix fun <V, E : Exception> Result<V, E>.getOrElse(fallback: V) = when (this) {
    is Result.Success -> value
    else -> fallback
}

fun <V, U, E : Exception> Result<V, E>.map(transform: (V) -> U): Result<U, E> = when (this) {
    is Result.Success -> Result.Success<U, E>(transform(value))
    is Result.Failure -> Result.Failure<U, E>(error)
}

fun <V, U, E : Exception> Result<V, E>.flatMap(transform: (V) -> Result<U, E>): Result<U, E> = when (this) {
    is Result.Success -> transform(value)
    is Result.Failure -> Result.Failure<U, E>(error)
}

fun <V, E : Exception, E2 : Exception> Result<V, E>.mapError(transform: (E) -> E2) = when (this) {
    is Result.Success -> Result.Success<V, E2>(value)
    is Result.Failure -> Result.Failure<V, E2>(transform(error))
}

fun <V, E : Exception, E2 : Exception> Result<V, E>.flatMapError(transform: (E) -> Result<V, E2>) = when (this) {
    is Result.Success -> Result.Success<V, E2>(value)
    is Result.Failure -> transform(error)
}

fun <V> Result<V, *>.any(predicate: (V) -> Boolean): Boolean = when (this) {
    is Result.Success -> predicate(value)
    is Result.Failure -> false
}

sealed class Result<out V, out E : Exception> {

    abstract operator fun component1(): V?
    abstract operator fun component2(): E?

    inline fun <X> fold(success: (V) -> X, failure: (E) -> X): X {
        return when (this) {
            is Success -> success(this.value)
            is Failure -> failure(this.error)
        }
    }

    abstract fun get(): V

    class Success<out V, out E : Exception>(val value: V) : Result<V, E>() {
        override fun component1(): V? = value
        override fun component2(): E? = null

        override fun get(): V = value

        override fun toString() = "[Success: $value]"

        override fun hashCode(): Int = run {
            println("hashCode succ")
            value?.hashCode() ?: 0
        }

        override fun equals(other: Any?): Boolean {
            if (this === other) return true
            return other is Success<*, *> && value == other.value
        }
    }

    class Failure<out V, out E : Exception>(val error: E) : Result<V, E>() {
        override fun component1(): V? = null
        override fun component2(): E? = error

        override fun get(): V = throw error

        fun getException(): E = error

        override fun toString() = "[Failure: $error]"

        override fun hashCode(): Int = error.hashCode()

        override fun equals(other: Any?): Boolean {
            if (this === other) return true
            return other is Failure<*, *> && error == other.error
        }
    }

    companion object {
        // Factory methods
        fun <E : Exception> error(ex: E) = Failure<Nothing, E>(ex)

         fun <V> of(value: V): Result<V, Exception> {
             return Success<V, Nothing>(value)
         }

        fun <V> of(f: () -> V): Result<V, Exception> = try {
            Success(f())
        } catch(ex: Exception) {
            Failure(ex)
        }
    }

}


fun main(args: Array<String>) {
    println("hello")
    val r1 = Result.of(42)
    println(r1.get())
    val r2: Result<Int?, Exception> = Result.of { null }
    println(r2.get())
    println(r2.map { if (it == null) 1 else 2 }.get())
}

