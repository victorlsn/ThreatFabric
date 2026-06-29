package com.threatfabric.assessment.storage

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.util.concurrent.Callable
import java.util.concurrent.CountDownLatch
import java.util.concurrent.Executors
import java.util.concurrent.TimeUnit
import kotlin.random.Random

class InMemoryKeyValueStoreTest {

    @Test
    fun `put and get store raw values by key`() {
        val store = InMemoryKeyValueStore()

        store.put("OSName", StoredValue.Text("Android"))
        store.put("DeviceCPUCount", StoredValue.Int32(8))

        assertEquals(StoredValue.Text("Android"), store["OSName"])
        assertEquals(StoredValue.Int32(8), store["DeviceCPUCount"])
    }

    @Test
    fun `typed put and get use adapters safely`() {
        val store = InMemoryKeyValueStore()

        store.put("OSVersion", 16, ValueAdapters.int)

        assertEquals(16, store.get("OSVersion", ValueAdapters.int))
        assertNull(store.get("OSVersion", ValueAdapters.string))
    }

    @Test
    fun `put updates existing key without duplicating indexes`() {
        val store = InMemoryKeyValueStore()

        store.put("RootedDevice", false, ValueAdapters.boolean)
        store.put("RootedDevice", true, ValueAdapters.boolean)

        assertEquals(StoredValue.Flag(true), store["RootedDevice"])
        assertEquals(1, store.size())
        assertEquals(listOf("RootedDevice"), store.entries().map { it.key })
    }

    @Test
    fun `remove deletes keys and returns removed value`() {
        val store = InMemoryKeyValueStore()
        store.put("DeveloperModeEnabled", false, ValueAdapters.boolean)

        assertEquals(StoredValue.Flag(false), store.remove("DeveloperModeEnabled"))
        assertNull(store["DeveloperModeEnabled"])
        assertNull(store.remove("DeveloperModeEnabled"))
    }

    @Test
    fun `random value returns null for empty store and an existing value otherwise`() {
        val random = object : Random() {
            override fun nextBits(bitCount: Int): Int = 0
        }
        val store = InMemoryKeyValueStore(random = random)

        assertNull(store.getRandomValue())

        store.put("OSVersion", StoredValue.Text("16"))
        store.put("OSName", StoredValue.Text("Android"))

        assertEquals(StoredValue.Text("16"), store.getRandomValue())
    }

    @Test
    fun `prefix search uses sorted set range scan and respects limits`() {
        val store = InMemoryKeyValueStore()
        store.put("DeviceCPUCount", StoredValue.Int32(8))
        store.put("DeviceManufacturer", StoredValue.Text("Google"))
        store.put("DeviceModel", StoredValue.Text("Pixel"))
        store.put("DeveloperModeEnabled", StoredValue.Flag(false))
        store.put("OSName", StoredValue.Text("Android"))

        assertEquals(
            listOf("DeviceCPUCount", "DeviceManufacturer", "DeviceModel"),
            store.getKeysByPrefix("Device"),
        )
        assertEquals(
            listOf("DeviceCPUCount", "DeviceManufacturer"),
            store.getKeysByPrefix("Device", limit = 2),
        )
        assertTrue(store.getKeysByPrefix("Unknown").isEmpty())
    }

    @Test
    fun `entries returns a stable sorted snapshot of current pairs`() {
        val store = InMemoryKeyValueStore()
        store.put("OSName", StoredValue.Text("Android"))
        store.put("OSVersion", StoredValue.Text("16"))

        val entries = store.entries()

        assertEquals(listOf("OSName", "OSVersion"), entries.map { it.key })
        assertEquals(listOf(StoredValue.Text("Android"), StoredValue.Text("16")), entries.map { it.value })
        assertTrue(entries.all { it.updatedAtMillis > 0 })
    }

    @Test
    fun `clear removes all in-memory state`() {
        val store = InMemoryKeyValueStore()
        store.put("OSName", StoredValue.Text("Android"))
        store.put("OSVersion", StoredValue.Text("16"))

        store.clear()

        assertEquals(0, store.size())
        assertTrue(store.entries().isEmpty())
        assertNull(store.getRandomValue())
    }

    @Test
    fun `concurrent readers and writers preserve consistent state`() {
        val store = InMemoryKeyValueStore()
        val threadCount = 8
        val itemsPerThread = 120
        val pool = Executors.newFixedThreadPool(threadCount)
        val startSignal = CountDownLatch(1)

        val futures = (0 until threadCount).map { worker ->
            pool.submit(Callable {
                startSignal.await()
                repeat(itemsPerThread) { index ->
                    val key = "worker$worker-item$index"
                    store.put(key, index, ValueAdapters.int)
                    if (index % 2 == 0) {
                        store.put(key, index * 10, ValueAdapters.int)
                    }
                    if (index % 5 == 0) {
                        store.remove(key)
                    } else {
                        assertTrue(store.contains(key))
                    }
                    store.getRandomValue()
                    store.getKeysByPrefix("worker$worker-item", limit = 10)
                }
            })
        }

        startSignal.countDown()
        futures.forEach { it.get(10, TimeUnit.SECONDS) }
        pool.shutdown()
        assertTrue(pool.awaitTermination(5, TimeUnit.SECONDS))

        val expectedPerWorker = (0 until itemsPerThread).count { it % 5 != 0 }
        assertEquals(threadCount * expectedPerWorker, store.size())

        repeat(threadCount) { worker ->
            val keysForWorker = store.getKeysByPrefix("worker$worker-item")
            assertEquals(expectedPerWorker, keysForWorker.size)
            assertFalse(keysForWorker.any { it.endsWith("item0") || it.endsWith("item5") })
            assertEquals(StoredValue.Int32(1), store["worker$worker-item1"])
            assertEquals(StoredValue.Int32(20), store["worker$worker-item2"])
        }
    }
}
