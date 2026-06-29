package com.threatfabric.assessment.storage.room

import androidx.room.Entity
import androidx.room.PrimaryKey
import com.threatfabric.assessment.storage.KeyValueEntry
import com.threatfabric.assessment.storage.StoredValue

@Entity(tableName = "stored_entries")
internal data class StoredEntryEntity(
    @PrimaryKey val entryKey: String,
    val kind: String,
    val payload: String,
    val adapterId: String?,
    val updatedAtMillis: Long,
)

internal fun StoredEntryEntity.toEntry(): KeyValueEntry =
    KeyValueEntry(
        key = entryKey,
        value = when (kind) {
            StoredValue.Kind.STRING.name -> StoredValue.Text(payload)
            StoredValue.Kind.INT.name -> StoredValue.Int32(payload.toInt())
            StoredValue.Kind.LONG.name -> StoredValue.Int64(payload.toLong())
            StoredValue.Kind.DOUBLE.name -> StoredValue.Decimal(payload.toDouble())
            StoredValue.Kind.BOOLEAN.name -> StoredValue.Flag(payload.toBooleanStrict())
            StoredValue.Kind.JSON.name -> StoredValue.Json(
                adapterId = requireNotNull(adapterId),
                payload = payload,
            )
            else -> error("Unsupported stored value kind: $kind")
        },
        updatedAtMillis = updatedAtMillis,
    )

internal fun KeyValueEntry.toEntity(): StoredEntryEntity {
    val (payload, adapterId) = when (val storedValue = value) {
        is StoredValue.Text -> storedValue.content to null
        is StoredValue.Int32 -> storedValue.content.toString() to null
        is StoredValue.Int64 -> storedValue.content.toString() to null
        is StoredValue.Decimal -> storedValue.content.toString() to null
        is StoredValue.Flag -> storedValue.content.toString() to null
        is StoredValue.Json -> storedValue.payload to storedValue.adapterId
    }

    return StoredEntryEntity(
        entryKey = key,
        kind = value.kind.name,
        payload = payload,
        adapterId = adapterId,
        updatedAtMillis = updatedAtMillis,
    )
}
