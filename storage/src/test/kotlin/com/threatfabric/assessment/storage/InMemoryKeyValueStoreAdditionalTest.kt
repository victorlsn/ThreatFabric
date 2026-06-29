package com.threatfabric.assessment.storage

import com.google.gson.reflect.TypeToken
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Assert.fail
import org.junit.Test
import kotlin.random.Random

class InMemoryKeyValueStoreAdditionalTest {

    @Test
    fun `store can be restored from initial entries snapshot`() {
        val snapshot = listOf(
            KeyValueEntry("beta", StoredValue.Text("two"), updatedAtMillis = 20L),
            KeyValueEntry("alpha", StoredValue.Int32(1), updatedAtMillis = 10L),
            KeyValueEntry("alphabet", StoredValue.Flag(true), updatedAtMillis = 30L),
        )
        val deterministicRandom = object : Random() {
            override fun nextBits(bitCount: Int): Int = 0
        }

        val store = InMemoryKeyValueStore(
            initialEntries = snapshot,
            random = deterministicRandom,
        )

        assertEquals(StoredValue.Int32(1), store["alpha"])
        assertEquals(listOf("alpha", "alphabet"), store.getKeysByPrefix("alp"))
        assertEquals(listOf("alpha", "alphabet", "beta"), store.entries().map { it.key })
        assertEquals(StoredValue.Text("two"), store.getRandomValue())
    }

    @Test
    fun `blank keys are rejected`() {
        val store = InMemoryKeyValueStore()

        try {
            store.put("   ", StoredValue.Text("invalid"))
            fail("Expected IllegalArgumentException for blank key")
        } catch (_: IllegalArgumentException) {
            Unit
        }
    }

    @Test
    fun `empty prefix returns sorted keys and limit zero returns empty`() {
        val store = InMemoryKeyValueStore()
        store.put("gamma", StoredValue.Text("3"))
        store.put("alpha", StoredValue.Text("1"))
        store.put("beta", StoredValue.Text("2"))

        assertEquals(listOf("alpha", "beta"), store.getKeysByPrefix("", limit = 2))
        assertTrue(store.getKeysByPrefix("a", limit = 0).isEmpty())
        assertTrue(store.entries(limit = 0).isEmpty())
    }

    @Test
    fun `gson adapter round trips custom values`() {
        val store = InMemoryKeyValueStore()
        val adapter = ValueAdapters.gson<NetworkSignal>(
            type = object : TypeToken<NetworkSignal>() {}.type,
            typeLabel = "NetworkSignal",
        )
        val expected = NetworkSignal("wifi", 4)

        store.put("signal", expected, adapter)

        assertEquals(expected, store.get("signal", adapter))
    }

    private data class NetworkSignal(
        val type: String,
        val level: Int,
    )
}
