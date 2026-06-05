package com.vlatkogalev.data.postgres.columns

import org.jetbrains.exposed.v1.core.ColumnType

class PostgresTextArrayColumnType : ColumnType<List<String>>() {
    override fun sqlType(): String = "TEXT[]"

    override fun valueFromDB(value: Any): List<String> = when (value) {
        is Array<*> -> value.filterNotNull().map { it.toString() }
        is List<*> -> value.filterNotNull().map { it.toString() }
        else -> emptyList()
    }

    override fun notNullValueToDB(value: List<String>): Any =
        value.toTypedArray()

    override fun nonNullValueToString(value: List<String>): String =
        "{${value.joinToString(",") { "\"$it\"" }}}"
}
