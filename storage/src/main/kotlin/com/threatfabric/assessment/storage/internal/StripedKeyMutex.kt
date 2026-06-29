package com.threatfabric.assessment.storage.internal

import java.util.concurrent.locks.ReentrantLock
import kotlin.concurrent.withLock

internal class StripedKeyMutex(stripes: Int = DEFAULT_STRIPE_COUNT) {
    private val locks = Array(stripes) { ReentrantLock() }

    fun <T> withLock(
        key: String,
        block: () -> T,
    ): T = lockFor(key).withLock(block)

    fun <T> withAllLocks(block: () -> T): T {
        locks.forEach(ReentrantLock::lock)
        return try {
            block()
        } finally {
            for (index in locks.lastIndex downTo 0) {
                locks[index].unlock()
            }
        }
    }

    private fun lockFor(key: String): ReentrantLock {
        val index = (key.hashCode() and Int.MAX_VALUE) % locks.size
        return locks[index]
    }

    private companion object {
        const val DEFAULT_STRIPE_COUNT = 64
    }
}
