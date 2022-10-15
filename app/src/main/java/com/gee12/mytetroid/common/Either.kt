/**
 * Copyright (C) 2019 Fernando Cejas Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.gee12.mytetroid.common

/**
 * Represents a value of one of two possible types (a disjoint union).
 * Instances of [Either] are either an instance of [Left] or [Right].
 * FP Convention dictates that [Left] is used for "failure"
 * and [Right] is used for "success".
 *
 * @see Left
 * @see Right
 */

typealias EitherBlock<L, R> = () -> Either<L, R>

sealed class Either<out L, out R> {
    /** * Represents the left side of [Either] class which by convention is a "Failure". */
    data class Left<out L>(val a: L) : Either<L, Nothing>()

    /** * Represents the right side of [Either] class which by convention is a "Success". */
    data class Right<out R>(val b: R) : Either<Nothing, R>()

    /**
     * Returns true if this is a Right, false otherwise.
     * @see Right
     */
    val isRight get() = this is Right<R>

    /**
     * Returns true if this is a Left, false otherwise.
     * @see Left
     */
    val isLeft get() = this is Left<L>

    /**
     * Creates a Left type.
     * @see Left
     */
    fun <L> left(a: L) = Either.Left(a)


    /**
     * Creates a Left type.
     * @see Right
     */
    fun <R> right(b: R) = Either.Right(b)

    /**
     * Applies fnL if this is a Left or fnR if this is a Right.
     * @see Left
     * @see Right
     */
    fun fold(onLeft: (L) -> Any, onRight: (R) -> Any): Any =
        when (this) {
            is Left -> onLeft(a)
            is Right -> onRight(b)
        }

    /**
     * Applies fnL if this is a Left or fnR if this is a Right.
     * @see Left
     * @see Right
     */
    inline fun <T> foldResult(onLeft: (L) -> T, onRight: (R) -> T): T =
        when (this) {
            is Left -> onLeft(a)
            is Right -> onRight(b)
        }
}

/**
 * Composes 2 functions
 * See <a href="https://proandroiddev.com/kotlins-nothing-type-946de7d464fb">Credits to Alex Hart.</a>
 */
fun <A, B, C> ((A) -> B).c(f: (B) -> C): (A) -> C = {
    f(this(it))
}

/**
 * Right-biased flatMap() FP convention which means that Right is assumed to be the default case
 * to operate on. If it is Left, operations like map, flatMap, ... return the Left value unchanged.
 */
inline fun <T, L, R> Either<L, R>.flatMap(fn: (R) -> Either<L, T>): Either<L, T> =
    when (this) {
        is Either.Left -> Either.Left(a)
        is Either.Right -> fn(b)
    }

/**
 * Right-biased map() FP convention which means that Right is assumed to be the default case
 * to operate on. If it is Left, operations like map, flatMap, ... return the Left value unchanged.
 */
fun <T, L, R> Either<L, R>.map(fn: (R) -> (T)): Either<L, T> = this.flatMap(fn.c(::right))

/** Returns the value from this `Right` or the given argument if this is a `Left`.
 *  Right(12).getOrElse(17) RETURNS 12 and Left(12).getOrElse(17) RETURNS 17
 */
fun <L, R> Either<L, R>.getOrElse(value: R): R =
    when (this) {
        is Either.Left -> value
        is Either.Right -> b
    }

/**
 * Left-biased onFailure() FP convention dictates that when this class is Left, it'll perform
 * the onFailure functionality passed as a parameter, but, overall will still return an either
 * object so you chain calls.
 */
inline fun <L, R> Either<L, R>.onFailure(fn: (failure: L) -> Unit): Either<L, R> =
    this.apply { if (this is Either.Left) fn(a) }

/**
 * Right-biased onSuccess() FP convention dictates that when this class is Right, it'll perform
 * the onSuccess functionality passed as a parameter, but, overall will still return an either
 * object so you chain calls.
 */
inline fun <L, R> Either<L, R>.onSuccess(fn: (success: R) -> Unit): Either<L, R> =
    this.apply { if (this is Either.Right) fn(b) }


fun <L, R> EitherBlock<L, R>.getFailure(): L? {
    return invoke().let { if (it is Either.Left) it.a else null }
}

fun <L, R> EitherBlock<L, R>.getSuccess(): R? {
    return invoke().let { if (it is Either.Right) it.b else null }
}

fun beforeLeft(vararg items: EitherBlock<Failure, *>): Failure? {
    var result: Failure? = null
    for (item in items) {
        val left = item.getFailure()
        if (left != null) {
            result = left
            break
        }
    }
    return result
}

fun <R> beforeRight(vararg items: EitherBlock<Failure, R>): R? {
    var result: R? = null
    for (item in items) {
        val right = item.getSuccess()
        if (right != null) {
            result = right
            break
        }
    }
    return result
}

fun <R : Any> R.toRight(): Either.Right<R> {
    return Either.Right(this)
}

fun <L : Failure> L.toLeft(): Either.Left<L> {
    return Either.Left(this)
}

fun <Left, This, Joined> Either<Left, This>.join(joined: Either<Left, Joined>): Either<Left, Pair<This, Joined>> =

    this.flatMap { thisResult ->
        joined.flatMap { joinedResult ->
            Pair(thisResult, joinedResult).toRight()
        }
    }


fun <T> runBeforeLeft(vararg items: () -> Either<Failure, T>): Either<Failure, Boolean> {
    return runBeforeLeft(items.toList())
}

fun <T> runBeforeLeft(items: List<() -> Either<Failure, T>>): Either<Failure, Boolean> {
    var result: Either<Failure, T>? = null
    for (item in items) {
        result = item()
        if (result.isLeft) {
            break
        }
    }
    return when (result) {
        is Either.Left -> result.a.toLeft()
        is Either.Right -> true.toRight()
        else -> true.toRight()
    }
}

suspend fun ifEitherSuspend(value: Boolean, block: suspend () -> Either<Failure, Boolean>): Either<Failure, Boolean> {
    return if (value) {
        block()
    } else {
        true.toRight()
    }
}

fun ifEither(value: Boolean, block: () -> Either<Failure, Boolean>): Either<Failure, Boolean> {
    return if (value) {
        block()
    } else {
        true.toRight()
    }
}
