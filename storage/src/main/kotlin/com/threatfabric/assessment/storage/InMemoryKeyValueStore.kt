package com.threatfabric.assessment.storage

import com.threatfabric.assessment.storage.internal.NoOpPersistenceDriver
import com.threatfabric.assessment.storage.internal.PersistenceDriver
import com.threatfabric.assessment.storage.internal.RandomKeyIndex
import com.threatfabric.assessment.storage.internal.StripedKeyMutex
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.ConcurrentSkipListSet
import kotlin.random.Random

class InMemoryKeyValueStore internal constructor(
    initialEntries: List<KeyValueEntry> = emptyList(),
    private val random: Random = Random.Default,
    private val persistenceDriver: PersistenceDriver = NoOpPersistenceDriver,
    private val clock: () -> Long = System::currentTimeMillis,
) : KeyValueStore {

    constructor() : this(
        initialEntries = emptyList(),
        random = Random.Default,
    )

    constructor(random: Random) : this(
        initialEntries = emptyList(),
        random = random,
    )

    private val valuesByKey = ConcurrentHashMap<String, StoredRecord>()
    private val sortedKeys = ConcurrentSkipListSet<String>()
    private val randomKeyIndex = RandomKeyIndex()
    private val keyMutex = StripedKeyMutex()

    init {
        initialEntries.forEach { entry ->
            valuesByKey[entry.key] = StoredRecord(entry.value, entry.updatedAtMillis)
            sortedKeys += entry.key
            randomKeyIndex.add(entry.key)
        }
    }

    override fun put(key: String, value: StoredValue) {
        require(key.isNotBlank()) { "Key must not be blank." }

        keyMutex.withLock(key) {
            val record = StoredRecord(value = value, updatedAtMillis = clock())
            val previous = valuesByKey.put(key, record)
            if (previous == null) {
                sortedKeys += key
                randomKeyIndex.add(key)
            }
            persistenceDriver.enqueueUpsert(record.toEntry(key))
        }
    }

    override fun get(key: String): StoredValue? = valuesByKey[key]?.value

    override fun remove(key: String): StoredValue? = keyMutex.withLock(key) {
        val removed = valuesByKey.remove(key) ?: return@withLock null
        sortedKeys.remove(key)
        randomKeyIndex.remove(key)
        persistenceDriver.enqueueDelete(key)
        removed.value
    }

    override fun getRandomValue(): StoredValue? {
        repeat(3) {
            val randomKey = randomKeyIndex.randomKey(random) ?: return null
            val record = valuesByKey[randomKey]
            if (record != null) {
                return record.value
            }
        }

        return valuesByKey.values.firstOrNull()?.value
    }

    override fun getKeysByPrefix(prefix: String, limit: Int): List<String> {
        if (limit <= 0) {
            return emptyList()
        }

        val hasPrefix = prefix.isNotEmpty()
        val result = ArrayList<String>()
        val keys = if (prefix.isEmpty()) {
            sortedKeys
        } else {
            sortedKeys.subSet(prefix, true, prefix.upperBound(), true)
        }

        for (key in keys) {
            if (hasPrefix && !key.startsWith(prefix)) {
                break
            }

            if (!valuesByKey.containsKey(key)) {
                continue
            }

            result += key
            if (result.size == limit) {
                break
            }
        }

        return result
    }

    override fun entries(limit: Int): List<KeyValueEntry> {
        if (limit <= 0) {
            return emptyList()
        }

        val result = ArrayList<KeyValueEntry>()
        for (key in sortedKeys) {
            val record = valuesByKey[key] ?: continue
            result += record.toEntry(key)
            if (result.size == limit) {
                break
            }
        }
        return result
    }

    override fun contains(key: String): Boolean = valuesByKey.containsKey(key)

    override fun size(): Int = valuesByKey.size

    override fun clear() {
        keyMutex.withAllLocks {
            valuesByKey.clear()
            sortedKeys.clear()
            randomKeyIndex.clear()
            persistenceDriver.enqueueClear()
        }
    }

    override fun flush() {
        persistenceDriver.flush()
    }

    override fun close() {
        persistenceDriver.close()
    }

    private data class StoredRecord(
        val value: StoredValue,
        val updatedAtMillis: Long,
    ) {
        fun toEntry(key: String): KeyValueEntry =
            KeyValueEntry(
                key = key,
                value = value,
                updatedAtMillis = updatedAtMillis,
            )
    }

    private fun String.upperBound(): String = this + '\uffff'
}
