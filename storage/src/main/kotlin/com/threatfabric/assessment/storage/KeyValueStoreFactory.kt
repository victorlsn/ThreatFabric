package com.threatfabric.assessment.storage

import android.content.Context
import androidx.room.Room
import com.threatfabric.assessment.storage.room.KeyValueDatabase
import com.threatfabric.assessment.storage.room.toEntry
import com.threatfabric.assessment.storage.room.RoomPersistenceDriver

object KeyValueStoreFactory {
    fun createInMemory(): KeyValueStore = InMemoryKeyValueStore()

    fun createPersistent(
        context: Context,
        databaseName: String = "threatfabric-key-value.db",
    ): KeyValueStore {
        val database = Room.databaseBuilder(
            context.applicationContext,
            KeyValueDatabase::class.java,
            databaseName,
        ).build()

        val initialEntries = database.entryDao().getAll().map { it.toEntry() }

        return InMemoryKeyValueStore(
            initialEntries = initialEntries,
            persistenceDriver = RoomPersistenceDriver(
                dao = database.entryDao(),
                databaseCloser = database::close,
            ),
        )
    }
}
