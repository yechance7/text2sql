package io.ybigta.text2sql.ingest.llmendpoint

import io.ybigta.text2sql.ingest.logic.schema_ingest.TableSchemaJson

internal interface TableEntitiesExtractionEndpoint {
    /**
     * called when ingesting schema
     * @param tableSchemaJson [TableSchemaJson.strongEntities] and [TableSchemaJson.weekEntities] may be empty
     * @return list of extracted entities
     */
    fun request(tableSchemaJson: TableSchemaJson): List<String>
}