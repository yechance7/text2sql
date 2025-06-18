package io.ybigta.text2sql.ingest.vectordb.tables

import io.ybigta.text2sql.exposed.pgvector.pgVector
import org.jetbrains.exposed.dao.id.IntIdTable

object TableDocEmbddingTbl : IntIdTable("table_doc_embedding", "table_doc_embedding_id") {
    val tableDoc = reference("table_doc_id", TableDocTbl)
    val embedding = pgVector("embedding", 1536)
    val data = text("data")

    val embeddingCategory = enumerationByName<EmbeddingCategory>("embedding_category", 40)

    enum class EmbeddingCategory {
        CONNECTED_TABLES, TABLE_NAME, DESCRIPTION, DESCRIPTION_DEPENDENCIES, ENTITY
    }
}