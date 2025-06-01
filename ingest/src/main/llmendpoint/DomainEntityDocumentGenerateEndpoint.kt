package io.ybigta.text2sql.ingest.llmendpoint

import dev.langchain4j.service.UserMessage
import dev.langchain4j.service.V
import io.ybigta.text2sql.ingest.logic.qa_ingest.DomainEntitiyMapping
import io.ybigta.text2sql.ingest.logic.qa_ingest.TableSelection

interface DomainEntityDocumentGenerateEndpoint {
    @UserMessage("""
    question: {{question}} 
    source_tables: {{tableSelections}}
    """)
    fun request(@V("question") question: String, @V("tableSelections") tableSelections: Set<TableSelection>): List<DomainEntitiyMapping>
}