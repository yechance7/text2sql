package vectordb

import io.ybigta.text2sql.exposed.pgvector.pgVector
import io.ybigta.text2sql.ingest.logic.qa_ingest.DomainEntitiyMapping
import io.ybigta.text2sql.ingest.logic.qa_ingest.StructuredQa
import kotlinx.serialization.json.Json
import org.jetbrains.exposed.dao.id.IntIdTable
import org.jetbrains.exposed.sql.json.jsonb

object QaTbl : IntIdTable("qa", "qa_id") {
    val question = text("question")
    val answer = text("answer")
    val structuredQa = jsonb<StructuredQa>("structured_qa", Json)

    data class Dto(
        val id: Int,
        val question: String,
        val answer: String,
        val structuredQa: StructuredQa
    )
}

object QaEmbeddingTbl : IntIdTable("qa_embedding", "qa_embedding_id") {
    val qaId = reference("qa_id", QaTbl)
    val embedding = pgVector("embedding", 1536)
    val embeddingTarget = enumerationByName<EmbeddingTarget>("embedding_target", 30)
    val data = text("data")

    enum class EmbeddingTarget {
        QUESTION, REQUESTED_ENTITIES, NORMALIZED_QUESTION, VARIATION
    }
}

object DomainEntityMappingTbl : IntIdTable("domain_entity_mapping", "domain_entity_mapping_id") {
    val qaId = reference("qa_id", QaTbl)
    val entityMapping = jsonb<DomainEntitiyMapping>("entity_mapping", Json)
}