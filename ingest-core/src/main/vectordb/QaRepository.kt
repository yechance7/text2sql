package vectordb

import dev.langchain4j.model.embedding.EmbeddingModel
import io.ybigta.text2sql.ingest.logic.qa_ingest.DomainEntitiyMapping
import io.ybigta.text2sql.ingest.logic.qa_ingest.StructuredQa
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.launch
import org.jetbrains.exposed.sql.*
import org.jetbrains.exposed.sql.transactions.transaction

class QaRepository(
    private val db: Database,
) {
    fun insertAndGetId(question: String, answer: String, structuredQa: StructuredQa): Int = transaction(db) {
        QaTbl.insertAndGetId {
            it[QaTbl.question] = question
            it[QaTbl.answer] = answer
            it[QaTbl.structuredQa] = structuredQa
        }.value
    }

    fun find(question: String, answer: String): Pair<Int, StructuredQa> = transaction(db) {
        QaTbl
            .select(QaTbl.id, QaTbl.structuredQa)
            .andWhere { QaTbl.question eq question }
            .andWhere { QaTbl.answer eq answer }
            .limit(1)
            .single()
            .let { rw -> Pair(rw[QaTbl.id].value, rw[QaTbl.structuredQa]) }
    }

    fun findAll(): List<QaTbl.Dto> = transaction(db) {
        QaTbl
            .selectAll()
            .map { rw ->
                QaTbl.Dto(
                    rw[QaTbl.id].value,
                    rw[QaTbl.question],
                    rw[QaTbl.answer],
                    rw[QaTbl.structuredQa],
                )
            }
    }

}

class QaEmbeddingRepository(
    private val db: Database,
    private val embeddingModel: EmbeddingModel,
    private val qaRepository: QaRepository
) {
    suspend fun insertAllTypes(question: String, answer: String, structuredQa: StructuredQa) = coroutineScope {
        val qaId = qaRepository.find(question, answer).first
        launch {
            val questionEmbedding: FloatArray = embeddingModel.embed(question).content().vector()
            transaction(db)
            {
                QaEmbeddingTbl.insert {
                    it[QaEmbeddingTbl.embedding] = questionEmbedding
                    it[QaEmbeddingTbl.embeddingTarget] = QaEmbeddingTbl.EmbeddingTarget.QUESTION
                    it[QaEmbeddingTbl.data] = question
                    it[QaEmbeddingTbl.qaId] = qaId
                }
            }
        }
        launch {
            val requestedEntitiesEmbedding: FloatArray = embeddingModel.embed(structuredQa.requestedEntities).content().vector()
            transaction(db)
            {
                QaEmbeddingTbl.insert {
                    it[QaEmbeddingTbl.embedding] = requestedEntitiesEmbedding
                    it[QaEmbeddingTbl.embeddingTarget] = QaEmbeddingTbl.EmbeddingTarget.REQUESTED_ENTITIES
                    it[QaEmbeddingTbl.data] = structuredQa.requestedEntities
                    it[QaEmbeddingTbl.qaId] = qaId
                }
            }
        }
        launch {
            val normalizedQuestionEmbedding: FloatArray = embeddingModel.embed(structuredQa.normalizedQuestion).content().vector()
            transaction(db)
            {
                QaEmbeddingTbl.insert {
                    it[QaEmbeddingTbl.embedding] = normalizedQuestionEmbedding
                    it[QaEmbeddingTbl.embeddingTarget] = QaEmbeddingTbl.EmbeddingTarget.NORMALIZED_QUESTION
                    it[QaEmbeddingTbl.data] = structuredQa.normalizedQuestion
                    it[QaEmbeddingTbl.qaId] = qaId
                }
            }
        }
    }
}

class DomainEntityMappingRepository(
    private val db: Database,
) {
    fun insertAndGetId(qaId: Int, entityMapping: DomainEntitiyMapping): Int = transaction(db) {
        DomainEntityMappingTbl.insertAndGetId {
            it[DomainEntityMappingTbl.qaId] = qaId
            it[DomainEntityMappingTbl.entityMapping] = entityMapping
        }.value
    }
}
