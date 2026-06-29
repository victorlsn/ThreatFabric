package com.threatfabric.assessment.storage

interface ValueAdapter<T : Any> {
    val id: String

    fun encode(value: T): StoredValue

    fun decode(value: StoredValue): T?
}
