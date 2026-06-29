package com.threatfabric.assessment.storage.room

import com.threatfabric.assessment.storage.KeyValueEntry
import com.threatfabric.assessment.storage.StoredValue
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import java.util.Collections
import java.util.concurrent.atomic.AtomicBoolean

class RoomPersistenceDriverTest {

    @Test
    fun `flush waits for queued operations to complete in order`() {
        val dao = RecordingStoredEntryDao()
        val closed = AtomicBoolean(false)
        val driver = RoomPersistenceDriver(
            dao = dao,
            databaseCloser = { closed.set(true) },
        )

        driver.enqueueUpsert(KeyValueEntry("alpha", StoredValue.Text("one"), 1L))
        driver.enqueueDelete("beta")
        driver.enqueueClear()
        driver.flush()

        assertEquals(
            listOf("upsert:alpha", "delete:beta", "clear"),
            dao.operations,
        )
        assertTrue(!closed.get())

        driver.close()
        assertTrue(closed.get())
    }

    @Test
    fun `close also drains queued commands before returning`() {
        val dao = RecordingStoredEntryDao()
        val driver = RoomPersistenceDriver(
            dao = dao,
            databaseCloser = { dao.operations += "closed" },
        )

        driver.enqueueUpsert(KeyValueEntry("alpha", StoredValue.Text("one"), 1L))
        driver.enqueueDelete("alpha")
        driver.close()

        assertEquals(
            listOf("upsert:alpha", "delete:alpha", "closed"),
            dao.operations,
        )
    }

    private class RecordingStoredEntryDao : StoredEntryDao {
        val operations = Collections.synchronizedList(mutableListOf<String>())

        override fun getAll(): List<StoredEntryEntity> = emptyList()

        override fun upsert(entry: StoredEntryEntity) {
            operations += "upsert:${entry.entryKey}"
        }

        override fun deleteByKey(key: String) {
            operations += "delete:$key"
        }

        override fun clearAll() {
            operations += "clear"
        }
    }
}
