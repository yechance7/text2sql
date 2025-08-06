package io.ybigta.text2sql.infer.server.repository

import io.ybigta.text2sql.ingest.TableDesc
import io.ybigta.text2sql.ingest.vectordb.tables.TableDocTbl
import org.jetbrains.exposed.sql.Database
import org.jetbrains.exposed.sql.SqlExpressionBuilder.eq
import org.jetbrains.exposed.sql.deleteWhere
import org.jetbrains.exposed.sql.transactions.transaction


class TblDocRepository(
    private val db: Database
) {
    /**
     * @return (table_description_id, table_description_json)
     */
    fun findAll(): List<Pair<Int, TableDesc>> = transaction(db) {
        TableDocTbl
            .select(TableDocTbl.schemaJson, TableDocTbl.id)
            .map { rs ->
                Pair(
                    rs[TableDocTbl.id].value,
                    rs[TableDocTbl.schemaJson]
                )
            }
    }

    fun findById(tblDocId: Int): TableDesc? = transaction(db) {
        TableDocTbl
            .select(TableDocTbl.schemaJson)
            .where { TableDocTbl.id eq tblDocId }
            .singleOrNull()
            ?.let { rs -> rs[TableDocTbl.schemaJson] }
    }

    fun deleteTblDoc(tblDocId: Int) = transaction(db) {
        TableDocTbl.deleteWhere { TableDocTbl.id eq tblDocId }
    }
}