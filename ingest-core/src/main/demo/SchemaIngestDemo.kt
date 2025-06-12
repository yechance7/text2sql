package io.ybigta.text2sql.ingest.demo

import io.ybigta.text2sql.ingest.SchemaIngester
import io.ybigta.text2sql.ingest.config.IngestConfig
import io.ybigta.text2sql.ingest.vectordb.TableDocEmbddingTbl
import io.ybigta.text2sql.ingest.vectordb.TableDocTbl
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.runBlocking
import org.jetbrains.exposed.sql.SchemaUtils
import org.jetbrains.exposed.sql.transactions.transaction
import java.nio.file.Path

fun main() {
    val configFilePath = Path.of("./.docs/ingest_config.yaml")
    val config = IngestConfig.fromConfigFile(configFilePath)

    val schemaDocGenerator = SchemaIngester(config)

    transaction(config.pgvector) {
        exec("""CREATE EXTENSION IF NOT EXISTS vector;""")
        SchemaUtils.create(TableDocTbl, TableDocEmbddingTbl)
    }

    runBlocking(Dispatchers.IO) {
        schemaDocGenerator.ingest()
    }
}