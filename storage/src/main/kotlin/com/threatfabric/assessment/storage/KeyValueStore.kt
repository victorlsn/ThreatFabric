package com.threatfabric.assessment.storage

interface KeyValueStore : AutoCloseable {
    fun put(key: String, value: StoredValue)

    fun <T : Any> put(
        key: String,
        value: T,
        adapter: ValueAdapter<T>,
    ) = put(key, adapter.encode(value))

    operator fun get(key: String): StoredValue?

    fun <T : Any> get(
        key: String,
        adapter: ValueAdapter<T>,
    ): T? = this[key]?.let(adapter::decode)

    fun remove(key: String): StoredValue?

    fun getRandomValue(): StoredValue?

    fun getKeysByPrefix(
        prefix: String,
        limit: Int = Int.MAX_VALUE,
    ): List<String>

    fun entries(limit: Int = Int.MAX_VALUE): List<KeyValueEntry>

    fun contains(key: String): Boolean

    fun size(): Int

    fun clear()

    fun flush()

    override fun close()
}
