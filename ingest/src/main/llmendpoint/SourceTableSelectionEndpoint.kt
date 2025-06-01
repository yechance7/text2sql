package io.ybigta.text2sql.ingest.llmendpoint

import dev.langchain4j.service.UserMessage
import dev.langchain4j.service.V
import io.ybigta.text2sql.ingest.logic.qa_ingest.TableSelection

interface SourceTableSelectionEndpoint {
    @UserMessage( """
    question: {{question}}
    corresponding_sql: {{answersql}}
    source_tables: {{tables}}
    domain_rules: {{rules}}
    """ )
    fun reqeust(@V("question") question: String,@V("answersql") answersql: String,@V("rules") rules: Set<String>,@V("tables") tables: Set<String>): Set<TableSelection>
}