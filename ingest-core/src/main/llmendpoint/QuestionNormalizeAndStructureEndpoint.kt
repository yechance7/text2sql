package io.ybigta.text2sql.ingest.llmendpoint

import dev.langchain4j.service.UserMessage
import dev.langchain4j.service.V
import io.ybigta.text2sql.ingest.logic.qa_ingest.StructuredQa

internal interface QuestionNormalizeAndStructureEndpoint {
    /**
     * give question(natural language) and anwser(SQL) pair to LLM
     * get normalized question, requested entities, data source, calculations&filter
     * called when ingesting qa
     */
    @UserMessage(
        """question: {{question}}, anwser:{{anwserSql}}"""
    )
    fun request(@V("question") question: String, @V("anwserSql") answerSql: String): StructuredQa

}