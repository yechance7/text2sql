package io.ybigta.text2sql.ingest.vectordb.repositories

import dev.langchain4j.data.embedding.Embedding
import dev.langchain4j.model.embedding.EmbeddingModel
import io.ybigta.text2sql.ingest.logic.schema_ingest.TableSchemaJson
import io.ybigta.text2sql.ingest.vectordb.tables.TableDocTbl
import kotlinx.serialization.json.Json
import org.jetbrains.exposed.sql.Database
import org.jetbrains.exposed.sql.insertAndGetId
import org.jetbrains.exposed.sql.transactions.transaction

internal class TableDocRepository(
    private val db: Database,
    private val embeddingModel: EmbeddingModel
) {
    fun insertAndGetId(schemaName: String, tableName: String, schemaDoc: TableSchemaJson): Int = transaction(db = db) {
        val embedding: Embedding = embeddingModel.embed(Json.encodeToString(schemaDoc)).content()
        TableDocTbl.insertAndGetId {
            it[TableDocTbl.schema] = schemaName
            it[TableDocTbl.table] = tableName
            it[TableDocTbl.schemaJson] = schemaDoc
        }.value
    }

    /**
     * 서로 다른 스키마에 동일이름 테이블 있을경우 오류 날 꺼임
     */
    fun findDocByTableName(tableName: String): TableSchemaJson? = transaction(db) {
        TableDocTbl
            .select(TableDocTbl.schemaJson)
            .where { TableDocTbl.table eq tableName }
            .firstOrNull()
            ?.let { rw -> rw[TableDocTbl.schemaJson] }
    }
}
