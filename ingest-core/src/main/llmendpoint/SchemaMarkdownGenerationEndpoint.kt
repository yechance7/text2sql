package io.ybigta.text2sql.ingest.llmendpoint

import io.ybigta.text2sql.ingest.logic.doc_gene.TableSchema
import kotlinx.serialization.json.JsonObject

internal interface SchemaMarkdownGenerationEndpoint {
    /**
     * auto-generate markdown schema documentation based on [tableSchema]
     *
     * @param tableSchema: Serialized json of [TableSchema]
     * @return markdown document about table schema
     */
    fun request(tableSchema: JsonObject): String
}