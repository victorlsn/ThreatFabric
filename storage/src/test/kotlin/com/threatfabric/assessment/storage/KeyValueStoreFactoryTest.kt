package com.threatfabric.assessment.storage

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import java.util.UUID
import java.util.concurrent.Callable
import java.util.concurrent.Executors
import java.util.concurrent.TimeUnit

@RunWith(RobolectricTestRunner::class)
class KeyValueStoreFactoryTest {

    private val context: Context = ApplicationProvider.getApplicationContext()

    @Test
    fun `persistent store reloads flushed entries`() {
        val databaseName = uniqueDatabaseName()
        context.deleteDatabase(databaseName)

        try {
            runInBackground {
                KeyValueStoreFactory.createPersistent(context, databaseName)
            }.use { firstStore ->
                firstStore.put("alpha", StoredValue.Text("one"))
                firstStore.put("beta", StoredValue.Int32(2))
                firstStore.flush()
            }

            runInBackground {
                KeyValueStoreFactory.createPersistent(context, databaseName)
            }.use { restoredStore ->
                assertEquals(StoredValue.Text("one"), restoredStore["alpha"])
                assertEquals(StoredValue.Int32(2), restoredStore["beta"])
                assertEquals(listOf("alpha", "beta"), restoredStore.entries().map { it.key })
            }
        } finally {
            context.deleteDatabase(databaseName)
        }
    }

    @Test
    fun `persistent store reflects remove and clear after reopen`() {
        val databaseName = uniqueDatabaseName()
        context.deleteDatabase(databaseName)

        try {
            runInBackground {
                KeyValueStoreFactory.createPersistent(context, databaseName)
            }.use { store ->
                store.put("alpha", StoredValue.Text("one"))
                store.put("beta", StoredValue.Text("two"))
                store.flush()
                store.remove("alpha")
                store.flush()
                store.clear()
                store.flush()
            }

            runInBackground {
                KeyValueStoreFactory.createPersistent(context, databaseName)
            }.use { restoredStore ->
                assertNull(restoredStore["alpha"])
                assertNull(restoredStore["beta"])
                assertEquals(0, restoredStore.size())
            }
        } finally {
            context.deleteDatabase(databaseName)
        }
    }

    private fun uniqueDatabaseName(): String = "test-store-${UUID.randomUUID()}.db"

    private fun <T> runInBackground(block: () -> T): T {
        val executor = Executors.newSingleThreadExecutor()
        return try {
            executor.submit(Callable(block)).get(10, TimeUnit.SECONDS)
        } finally {
            executor.shutdownNow()
        }
    }
}
