package com.threatfabric.assessment.storage

import com.google.gson.Gson
import java.lang.reflect.Type

object ValueAdapters {
    val string = object : ValueAdapter<String> {
        override val id: String = "builtin:string"

        override fun encode(value: String): StoredValue = StoredValue.Text(value)

        override fun decode(value: StoredValue): String? = (value as? StoredValue.Text)?.content
    }

    val int = object : ValueAdapter<Int> {
        override val id: String = "builtin:int"

        override fun encode(value: Int): StoredValue = StoredValue.Int32(value)

        override fun decode(value: StoredValue): Int? = (value as? StoredValue.Int32)?.content
    }

    val long = object : ValueAdapter<Long> {
        override val id: String = "builtin:long"

        override fun encode(value: Long): StoredValue = StoredValue.Int64(value)

        override fun decode(value: StoredValue): Long? = (value as? StoredValue.Int64)?.content
    }

    val double = object : ValueAdapter<Double> {
        override val id: String = "builtin:double"

        override fun encode(value: Double): StoredValue = StoredValue.Decimal(value)

        override fun decode(value: StoredValue): Double? = (value as? StoredValue.Decimal)?.content
    }

    val boolean = object : ValueAdapter<Boolean> {
        override val id: String = "builtin:boolean"

        override fun encode(value: Boolean): StoredValue = StoredValue.Flag(value)

        override fun decode(value: StoredValue): Boolean? = (value as? StoredValue.Flag)?.content
    }

    fun <T : Any> gson(
        type: Type,
        typeLabel: String,
        gson: Gson = Gson(),
    ): ValueAdapter<T> = object : ValueAdapter<T> {
        override val id: String = "gson:$typeLabel"

        override fun encode(value: T): StoredValue =
            StoredValue.Json(
                adapterId = id,
                payload = gson.toJson(value, type),
            )

        override fun decode(value: StoredValue): T? {
            val jsonValue = value as? StoredValue.Json ?: return null
            if (jsonValue.adapterId != id) {
                return null
            }
            return gson.fromJson(jsonValue.payload, type)
        }
    }
}
