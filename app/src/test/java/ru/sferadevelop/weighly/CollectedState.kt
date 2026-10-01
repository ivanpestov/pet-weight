package ru.sferadevelop.weighly

import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.UnconfinedTestDispatcher

/**
 * Reads screen state with a collector attached, the way a screen on display attaches one. Without
 * it a `WhileSubscribed` state flow never leaves its initial value.
 */
fun <T> TestScope.collectedState(state: StateFlow<T>): T {
    backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) { state.collect { } }
    return state.value
}
