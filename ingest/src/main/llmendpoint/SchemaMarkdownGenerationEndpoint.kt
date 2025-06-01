package io.ybigta.text2sql.ingest.llmendpoint

import io.ybigta.text2sql.ingest.logic.schema_ingest.TableSchema
import kotlinx.serialization.json.JsonObject

interface SchemaMarkdownGenerationEndpoint {
    /**
     * @param tableSchmea: Serialized json of [TableSchema]
     * @return markdown document about table schema
     */
    fun request(tableSchmea: JsonObject): String
}