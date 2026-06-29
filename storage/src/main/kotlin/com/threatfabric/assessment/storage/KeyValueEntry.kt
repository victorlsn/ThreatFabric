package com.threatfabric.assessment.storage

data class KeyValueEntry(
    val key: String,
    val value: StoredValue,
    val updatedAtMillis: Long,
)
