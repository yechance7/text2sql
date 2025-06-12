package io.ybigta.text2sql.exposed.pgvector

import org.jetbrains.exposed.sql.Column
import org.jetbrains.exposed.sql.ColumnType
import org.jetbrains.exposed.sql.Table
import org.jetbrains.exposed.sql.statements.api.PreparedStatementApi
import org.postgresql.util.PGobject

class PgVectorColumnType(private val size: Int) : ColumnType<FloatArray>() {
    override fun sqlType(): String = "vector($size)"

    override fun valueFromDB(value: Any): FloatArray? {
        return when (value) {
            is PGobject -> {
                valueFromDB(value.value!!)
            }
            is String -> {
                value
                    .drop(1) // remove '[' at start of string
                    .dropLast(1) // remove ']' at end of string
                    .split(",")
                    .map(String::toFloat)
                    .toFloatArray()
            }
            else -> error("[valueFromDB] unexpected type: ${value::class.simpleName}")
        }
    }

    override fun notNullValueToDB(value: FloatArray): Any {
        return "[${value.joinToString()}]"
    }

    override fun setParameter(stmt: PreparedStatementApi, index: Int, value: Any?) {
        val parameterValue: PGobject? = value?.let {
            PGobject().apply {
                type = "vector"
                this.value = value as? String
            }
        }
        super.setParameter(stmt, index, parameterValue)
    }

}

fun Table.pgVector(name: String, size: Int): Column<FloatArray> = registerColumn(name, PgVectorColumnType(size))
