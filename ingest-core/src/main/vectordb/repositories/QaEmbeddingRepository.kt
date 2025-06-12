package io.ybigta.text2sql.ingest.vectordb.repositories

import dev.langchain4j.model.embedding.EmbeddingModel
import io.ybigta.text2sql.ingest.logic.qa_ingest.StructuredQa
import io.ybigta.text2sql.ingest.vectordb.tables.QaEmbeddingTbl
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.launch
import org.jetbrains.exposed.sql.Database
import org.jetbrains.exposed.sql.insert
import org.jetbrains.exposed.sql.transactions.transaction

internal class QaEmbeddingRepository(
    private val db: Database,
    private val embeddingModel: EmbeddingModel,
) {
    suspend fun insertAllTypes(qaId: Int, structuredQa: StructuredQa) = coroutineScope {
        launch {
            val questionEmbedding: FloatArray = embeddingModel.embed(structuredQa.question).content().vector()
            transaction(db)
            {
                QaEmbeddingTbl.insert {
                    it[QaEmbeddingTbl.embedding] = questionEmbedding
                    it[QaEmbeddingTbl.embeddingTarget] = QaEmbeddingTbl.EmbeddingTarget.QUESTION
                    it[QaEmbeddingTbl.data] = structuredQa.question
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