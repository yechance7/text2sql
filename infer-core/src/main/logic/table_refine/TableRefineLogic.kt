package io.ybigta.text2sql.infer.core.logic.table_refine

import dev.langchain4j.service.UserMessage
import dev.langchain4j.service.V
import io.ybigta.text2sql.ingest.TableDesc
import io.ybigta.text2sql.ingest.TableName
import kotlinx.coroutines.coroutineScope

class TableRefineLogic(
    private val tableRefinementEndpoint: TableRefinementEndpoint
) {
    /**
     * filter necessary table to answer user question(by LLM)
     */
    suspend fun refineTableDesc(
        question: String,
        tableDescList: List<TableDesc>,
        businessRule: String,
    ): List<TableName> = coroutineScope {
        val selectedTableNames = tableRefinementEndpoint.request(question, tableDescList, businessRule)

        return@coroutineScope selectedTableNames
    }
}

interface TableRefinementEndpoint {

    @UserMessage(
        """
    question: {{question}}
    businessRule: {{businessRule}}
    tableDocs: {{tableDocs}}
    """
    )
    fun request(
        @V("question") question: String,
        @V("tableDocs") tableDescList: List<TableDesc>,
        @V("businessRule") businessRule: String,
    ): List<TableName>
}