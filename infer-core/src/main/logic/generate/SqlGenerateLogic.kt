package io.ybigta.text2sql.infer.core.logic.generate

import dev.langchain4j.service.UserMessage
import dev.langchain4j.service.V
import io.ybigta.text2sql.infer.core.Question
import io.ybigta.text2sql.infer.core.logic.qa_retrieve.QaRetrieveResult
import io.ybigta.text2sql.ingest.TableDesc
import org.slf4j.LoggerFactory

/**
 * @param dialect dialect name of sql that will be generated
 */
class SqlGenerateLogic(
    private val sqlGenerationEndpoint: SqlGenerationEndpoint,
    private val dialect: String
) {
    private val logger = LoggerFactory.getLogger(this::class.java)

    suspend fun generateCode(
        question: Question,
        tableSchemaList: List<TableDesc>,
        qaList: List<QaRetrieveResult>,
    ): String {
        logger.info("requesting sql generation for question: {}", question.question)
        return sqlGenerationEndpoint.request(
            question.question,
            tableSchemaList,
            qaList,
            dialect
        )
    }
}

interface SqlGenerationEndpoint {
    @UserMessage(
        """
    user_qustion: {{question}}
    table_desc: {{table_desc}}
    examples: {{examples}}
    dialect: {{dialect}}
    """
    )
    fun request(
        @V("question") question: String,
        @V("table_desc") tableSchemaList: List<TableDesc>,
        @V("examples") qaList: List<QaRetrieveResult>,
        @V("dialect") dialect: String
    ): String
}