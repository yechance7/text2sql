package io.ybigta.text2sql.ingest.vectordb

import dev.langchain4j.model.embedding.EmbeddingModel
import io.ybigta.text2sql.ingest.logic.schema_ingest.TableSchemaJson
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.launch
import kotlinx.serialization.json.Json
import org.jetbrains.exposed.dao.id.IntIdTable
import org.jetbrains.exposed.sql.Database
import org.jetbrains.exposed.sql.insertAndGetId
import org.jetbrains.exposed.sql.json.jsonb
import org.jetbrains.exposed.sql.transactions.experimental.newSuspendedTransaction
import pgVector

object TableDocTbl : IntIdTable("table_doc", "table_doc_id") {
    val schema = varchar("schema_name", 255)
    val table = varchar("table_name", 255)
    val schemaJson = jsonb<TableSchemaJson>("schema_json", Json)
}

object TableDocEmbddingTbl : IntIdTable("table_doc_embedding", "table_doc_embedding_id") {
    val tableDoc = reference("table_doc_id", TableDocTbl)
    val embedding = pgVector("embedding", 1536)
    val data = text("data")

    val embeddingCategory = enumerationByName<EmbeddingCategory>("embedding_category", 40)

    enum class EmbeddingCategory {
        CONNECTED_TABLES, TABLE_NAME, DESCRIPTION, DESCRIPTION_DEPENDENCIES, ENTITY
    }
}

class TableDocEmbeddingRepository(
    private val db: Database,
    private val embeddingModel: EmbeddingModel
) {
    suspend fun insertDescriptionCategory(tableDocId: Int, tableSchemaJson: TableSchemaJson) = newSuspendedTransaction(db = db) {
        val description = tableSchemaJson.toDescription()
        val embedding = embeddingModel.embed(description).content().vector()

        TableDocEmbddingTbl.insertAndGetId {
            it[TableDocEmbddingTbl.tableDoc] = tableDocId
            it[TableDocEmbddingTbl.embedding] = embedding
            it[TableDocEmbddingTbl.data] = description
            it[TableDocEmbddingTbl.embeddingCategory] = TableDocEmbddingTbl.EmbeddingCategory.DESCRIPTION
        }
    }

    suspend fun insertDescriptionDependenciesCategory(tableDocId: Int, tableSchemaJson: TableSchemaJson) = newSuspendedTransaction(db = db) {
        val descriptionWithDependencies = tableSchemaJson.toDescriptionWithDependencies()
        val embedding = embeddingModel.embed(descriptionWithDependencies).content().vector()
        TableDocEmbddingTbl.insertAndGetId {
            it[TableDocEmbddingTbl.tableDoc] = tableDocId
            it[TableDocEmbddingTbl.embedding] = embedding
            it[TableDocEmbddingTbl.data] = descriptionWithDependencies
            it[TableDocEmbddingTbl.embeddingCategory] = TableDocEmbddingTbl.EmbeddingCategory.DESCRIPTION_DEPENDENCIES
        }
    }

    suspend fun insertTableNameCategory(tableDocId: Int, tableSchemaJson: TableSchemaJson) = newSuspendedTransaction(db = db) {
        val embedding = embeddingModel.embed(tableSchemaJson.name).content().vector()
        TableDocEmbddingTbl.insertAndGetId {
            it[TableDocEmbddingTbl.tableDoc] = tableDocId
            it[TableDocEmbddingTbl.embedding] = embedding
            it[TableDocEmbddingTbl.data] = tableSchemaJson.name
            it[TableDocEmbddingTbl.embeddingCategory] = TableDocEmbddingTbl.EmbeddingCategory.TABLE_NAME
        }
    }


    suspend fun insertConnectedTablesCategory(tableDocId: Int, tableSchemaJson: TableSchemaJson) = newSuspendedTransaction(db = db) {
        val connectedTables = Json.encodeToString(tableSchemaJson.connectedTables)
        val embedding = embeddingModel.embed(connectedTables).content().vector()
        TableDocEmbddingTbl.insertAndGetId {
            it[TableDocEmbddingTbl.tableDoc] = tableDocId
            it[TableDocEmbddingTbl.embedding] = embedding
            it[TableDocEmbddingTbl.data] = connectedTables
            it[TableDocEmbddingTbl.embeddingCategory] = TableDocEmbddingTbl.EmbeddingCategory.CONNECTED_TABLES
        }
    }

    suspend fun insertEntityCategory(tableDocId: Int, tableSchemaJson: TableSchemaJson) = newSuspendedTransaction(db = db) {
        ((tableSchemaJson.strongEntities) + (tableSchemaJson.weekEntities)).forEach { entity ->
            val embedding = embeddingModel.embed(entity).content().vector()
            TableDocEmbddingTbl.insertAndGetId {
                it[TableDocEmbddingTbl.tableDoc] = tableDocId
                it[TableDocEmbddingTbl.embedding] = embedding
                it[TableDocEmbddingTbl.data] = entity
                it[TableDocEmbddingTbl.embeddingCategory] = TableDocEmbddingTbl.EmbeddingCategory.ENTITY
            }
        }
    }

    suspend fun insertAllCategories(tableDocId: Int, tableSchemaJson: TableSchemaJson) = coroutineScope {
        launch { insertDescriptionCategory(tableDocId, tableSchemaJson) }
        launch { insertDescriptionDependenciesCategory(tableDocId, tableSchemaJson) }
        launch { insertTableNameCategory(tableDocId, tableSchemaJson) }
        launch { insertConnectedTablesCategory(tableDocId, tableSchemaJson) }
        insertEntityCategory(tableDocId, tableSchemaJson)
    }
}