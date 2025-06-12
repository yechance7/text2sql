package io.ybigta.text2sql.ingest.vectordb.tables

import io.ybigta.text2sql.exposed.pgvector.pgVector
import org.jetbrains.exposed.dao.id.IntIdTable

internal object QaEmbeddingTbl : IntIdTable("qa_embedding", "qa_embedding_id") {
    val qaId = reference("qa_id", QaTbl)
    val embedding = pgVector("embedding", 1536)
    val embeddingTarget = enumerationByName<EmbeddingTarget>("embedding_target", 30)
    val data = text("data")

    enum class EmbeddingTarget {
        QUESTION, REQUESTED_ENTITIES, NORMALIZED_QUESTION, VARIATION
    }
}