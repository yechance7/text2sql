package io.ybigta.text2sql.ingest.vectordb

import dev.langchain4j.data.embedding.Embedding
import dev.langchain4j.model.embedding.EmbeddingModel
import io.ybigta.text2sql.ingest.logic.schema_ingest.TableSchemaJson
import kotlinx.serialization.json.Json
import org.jetbrains.exposed.sql.Database
import org.jetbrains.exposed.sql.insertAndGetId
import org.jetbrains.exposed.sql.transactions.experimental.newSuspendedTransaction
import org.jetbrains.exposed.sql.transactions.transaction

class TableSchemaDocRepository(
    private val db: Database,
    private val embeddingModel: EmbeddingModel
) {
    suspend fun insertAndGetId(schemaName: String, tableName: String, schemaDoc: TableSchemaJson): Int = newSuspendedTransaction(db = db) {
        val embedding: Embedding = embeddingModel.embed(Json.encodeToString(schemaDoc)).content()
        TableSchemaDocTbl.insertAndGetId {
            it[TableSchemaDocTbl.schema] = schemaName
            it[TableSchemaDocTbl.table] = tableName
            it[TableSchemaDocTbl.data] = schemaDoc
            it[TableSchemaDocTbl.embedding] = embedding.vector()
        }.value
    }

    /**
     * 서로 다른 스키마에 동일이름 테이블 있을경우 오류 날 꺼임
     */
    fun findDocByTableName(tableName: String): TableSchemaJson = transaction(db) {
        TableSchemaDocTbl
            .select(TableSchemaDocTbl.data)
            .where { TableSchemaDocTbl.table eq tableName }
            .first()
            .let { rw -> rw[TableSchemaDocTbl.data] }
    }

    fun findAll(): List<Pair<String, TableSchemaJson>> = transaction(db) {
        TableSchemaDocTbl
            .select(TableSchemaDocTbl.schema, TableSchemaDocTbl.data)
            .map { rw -> Pair(rw[TableSchemaDocTbl.schema], rw[TableSchemaDocTbl.data]) }
    }

}