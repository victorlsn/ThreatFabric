package com.threatfabric.assessment.storage.internal

import java.util.concurrent.locks.ReentrantReadWriteLock
import kotlin.concurrent.read
import kotlin.concurrent.write
import kotlin.random.Random

internal class RandomKeyIndex {
    private val keys = ArrayList<String>()
    private val indexesByKey = HashMap<String, Int>()
    private val lock = ReentrantReadWriteLock()

    fun add(key: String) {
        lock.write {
            if (indexesByKey.containsKey(key)) {
                return
            }
            indexesByKey[key] = keys.size
            keys += key
        }
    }

    fun remove(key: String) {
        lock.write {
            val removedIndex = indexesByKey.remove(key) ?: return
            val lastIndex = keys.lastIndex
            val lastKey = keys[lastIndex]

            if (removedIndex != lastIndex) {
                keys[removedIndex] = lastKey
                indexesByKey[lastKey] = removedIndex
            }

            keys.removeAt(lastIndex)
        }
    }

    fun randomKey(random: Random): String? = lock.read {
        if (keys.isEmpty()) {
            return@read null
        }
        keys[random.nextInt(keys.size)]
    }

    fun clear() {
        lock.write {
            keys.clear()
            indexesByKey.clear()
        }
    }
}
