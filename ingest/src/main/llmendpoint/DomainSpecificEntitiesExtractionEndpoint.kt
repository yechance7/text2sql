package io.ybigta.text2sql.ingest.llmendpoint

import io.ybigta.text2sql.ingest.logic.qa_ingest.TableSelection

interface DomainSpecificEntitiesExtractionEndpoint {
    /**
     *
     * @return extracted entities
     */
    fun reqeust(tableselections: Set<TableSelection>): Set<String>
}