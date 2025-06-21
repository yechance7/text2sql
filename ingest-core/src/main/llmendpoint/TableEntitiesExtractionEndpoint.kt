package io.ybigta.text2sql.ingest.llmendpoint

import io.ybigta.text2sql.ingest.TableDesc


interface TableEntitiesExtractionEndpoint {
    /**
     * called when ingesting schema
     * @param tableDesc [TableDesc.strongEntities] and [TableDesc.weekEntities] may be empty
     * @return list of extracted entities
     */
    fun request(tableDesc: TableDesc): List<String>
}