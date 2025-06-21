package io.ybigta.text2sql.ingest.llmendpoint

import io.ybigta.text2sql.ingest.TableDesc


interface StructureSchemaDocEndPoint {

    /**
     * convert table schema markdown doc to json(structured type) with extracting meaningful information
     * @param makrdown content of markdown doc
     * @return field [TableDesc.strongEntities] and [TableDesc.weekEntities] will be empty
     * use [TableEntitiesExtractionEndpoint] to request entities
     *
     */
    fun request(makrdown: String): TableDesc
}