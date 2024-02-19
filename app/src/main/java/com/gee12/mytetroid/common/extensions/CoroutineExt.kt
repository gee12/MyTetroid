package com.gee12.mytetroid.common.extensions

import kotlinx.coroutines.*


suspend fun <T> withMain(block: suspend CoroutineScope.() -> T): T {
    return withContext(
        context = Dispatchers.Main,
        block = block,
    )
}

suspend fun <T> withIo(block: suspend CoroutineScope.() -> T): T {
    return withContext(
        context = Dispatchers.IO,
        block = block,
    )
}

suspend fun <T> withComputation(block: suspend CoroutineScope.() -> T): T {
    return withContext(
        context = Dispatchers.Default,
        block = block,
    )
}

context(CoroutineScope)
fun launchOnMain(block: suspend CoroutineScope.() -> Unit): Job {
    return launch(
        context = Dispatchers.Main,
        block = block,
    )
}

context(CoroutineScope)
fun launchOnIo(
    coroutineDispatcher: CoroutineDispatcher = Dispatchers.IO,
    block: suspend CoroutineScope.() -> Unit
): Job {
    return launch(
        context = coroutineDispatcher,
        block = block,
    )
}