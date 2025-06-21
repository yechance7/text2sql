package io.ybigta.text2sql.infer.core.logic.table_refine

import dev.langchain4j.service.UserMessage
import dev.langchain4j.service.V
import io.ybigta.text2sql.ingest.DomainEntitiyMapping
import io.ybigta.text2sql.ingest.TableDesc
import kotlinx.coroutines.coroutineScope

internal class TableRefineLogic(
    private val tableRefinementEndpoint: TableRefinementEndpoint
) {
    suspend fun refineTableDoc(
        normalizedQ: String,
        domainEntitiyMappings: List<DomainEntitiyMapping>,
        businessRule: String,
        tableDocs: List<TableDesc>
    ) = coroutineScope {
        val result = tableRefinementEndpoint.request(normalizedQ, domainEntitiyMappings, businessRule, tableDocs)

    }
}

internal interface TableRefinementEndpoint {

    @UserMessage(
        """
    normalizedQ: {{normalizedQ}}
    domainMappings:{{domainMappings}}
    businessRule: {{businessRule}}
    tableDocs: {{tableDocs}}
    """
    )
    fun request(
        @V("normalizedQ") normalizedQ: String,
        @V("domainMappings") domainMappings: List<DomainEntitiyMapping>,
        @V("businessRule") businessRule: String,
        @V("tableDocs") tableDocs: List<TableDesc>
    )
}