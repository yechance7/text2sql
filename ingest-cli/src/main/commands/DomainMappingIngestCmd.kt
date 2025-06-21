package io.ybigta.text2sql.ingest.cli.commands

import com.github.ajalt.clikt.core.CliktCommand
import com.github.ajalt.clikt.parameters.options.option
import com.github.ajalt.clikt.parameters.options.required
import com.github.ajalt.clikt.parameters.types.path
import io.ybigta.text2sql.ingest.DomainMappingIngester
import io.ybigta.text2sql.ingest.config.IngestConfig
import kotlinx.coroutines.newFixedThreadPoolContext
import kotlinx.coroutines.runBlocking
import java.nio.file.Path

internal class DomainMappingIngestCmd : CliktCommand("ingest-domain-mapping") {
    private val configFile: Path by option("-c", "--config", help = "path of ingest_config.yaml").path().required()

    override fun run() {
        val config = IngestConfig.fromConfigFile(configFile)
        val domainIngester = DomainMappingIngester.fromConfig(config)
        val dispatcher = newFixedThreadPoolContext(30, "worker")


        // transaction(config.pgvector) {
        //     exec("""CREATE EXTENSION IF NOT EXISTS vector;""") // load pgvector extension
        //     SchemaUtils.create(QaTbl, TableDocTbl, QaEmbeddingTbl, DomainEntityMappingTbl) // create table if not exists
        // }

        runBlocking(dispatcher) { domainIngester.ingest() }
    }
}