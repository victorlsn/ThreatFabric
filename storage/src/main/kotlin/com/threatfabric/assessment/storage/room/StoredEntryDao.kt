package com.threatfabric.assessment.storage.room

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query

@Dao
internal interface StoredEntryDao {
    @Query("SELECT * FROM stored_entries ORDER BY entryKey ASC")
    fun getAll(): List<StoredEntryEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    fun upsert(entry: StoredEntryEntity)

    @Query("DELETE FROM stored_entries WHERE entryKey = :key")
    fun deleteByKey(key: String)

    @Query("DELETE FROM stored_entries")
    fun clearAll()
}
