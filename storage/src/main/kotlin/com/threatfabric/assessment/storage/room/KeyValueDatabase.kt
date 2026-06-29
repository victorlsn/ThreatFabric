package com.threatfabric.assessment.storage.room

import androidx.room.Database
import androidx.room.RoomDatabase

@Database(
    entities = [StoredEntryEntity::class],
    version = 1,
    exportSchema = false,
)
internal abstract class KeyValueDatabase : RoomDatabase() {
    abstract fun entryDao(): StoredEntryDao
}
