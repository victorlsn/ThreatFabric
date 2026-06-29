package com.threatfabric.assessment.storage

sealed interface StoredValue {
    val kind: Kind

    enum class Kind {
        STRING,
        INT,
        LONG,
        DOUBLE,
        BOOLEAN,
        JSON,
    }

    data class Text(val content: String) : StoredValue {
        override val kind: Kind = Kind.STRING
    }

    data class Int32(val content: Int) : StoredValue {
        override val kind: Kind = Kind.INT
    }

    data class Int64(val content: Long) : StoredValue {
        override val kind: Kind = Kind.LONG
    }

    data class Decimal(val content: Double) : StoredValue {
        override val kind: Kind = Kind.DOUBLE
    }

    data class Flag(val content: Boolean) : StoredValue {
        override val kind: Kind = Kind.BOOLEAN
    }

    data class Json(
        val adapterId: String,
        val payload: String,
    ) : StoredValue {
        override val kind: Kind = Kind.JSON
    }
}
