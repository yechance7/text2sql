package io.ybigta.text2sql.infer.server.repository.utils

import org.jetbrains.exposed.sql.Column
import org.jetbrains.exposed.sql.Table
import org.postgresql.util.PGobject

internal class PGEnum<T : Enum<T>>(enumTypeName: String, enumValue: T?) : PGobject() {
    init {
        value = enumValue?.name
        type = enumTypeName
    }
}

internal inline fun <reified T : Enum<T>> Table.pgEnumeration(
    columnName: String,
    enumName: String,
): Column<T> = customEnumeration(
    name = columnName,
    fromDb = { value -> enumValueOf<T>(value as String) },
    toDb = { PGEnum(enumName, it) }
)
