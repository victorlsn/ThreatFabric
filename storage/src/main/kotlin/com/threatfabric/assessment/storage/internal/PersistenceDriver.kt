package com.threatfabric.assessment.storage.internal

import com.threatfabric.assessment.storage.KeyValueEntry

internal interface PersistenceDriver : AutoCloseable {
    fun enqueueUpsert(entry: KeyValueEntry)

    fun enqueueDelete(key: String)

    fun enqueueClear()

    fun flush()

    override fun close()
}

internal object NoOpPersistenceDriver : PersistenceDriver {
    override fun enqueueUpsert(entry: KeyValueEntry) = Unit

    override fun enqueueDelete(key: String) = Unit

    override fun enqueueClear() = Unit

    override fun flush() = Unit

    override fun close() = Unit
}
