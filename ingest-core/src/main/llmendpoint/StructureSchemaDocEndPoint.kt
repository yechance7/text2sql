package io.ybigta.text2sql.ingest.llmendpoint

import io.ybigta.text2sql.ingest.logic.schema_ingest.TableSchemaJson

interface StructureSchemaDocEndPoint {

    /**
     * convert table schema markdown doc to json(structured type) with extracting meaningful information
     * @param makrdown content of markdown doc
     * @return field [TableSchemaJson.strongEntities] and [TableSchemaJson.weekEntities] will be empty
     * use [TableEntitiesExtractionEndpoint] to request entities
     *
     */
    fun request(makrdown: String): TableSchemaJson
}