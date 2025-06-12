package io.ybigta.text2sql.ingest.cli.commands

import com.github.ajalt.clikt.core.CliktCommand
import com.github.ajalt.clikt.parameters.options.option
import com.github.ajalt.clikt.parameters.options.required
import com.github.ajalt.clikt.parameters.types.path
import io.ybigta.text2sql.ingest.SchemaDocGenerator
import io.ybigta.text2sql.ingest.config.IngestConfig
import kotlinx.coroutines.newFixedThreadPoolContext
import kotlinx.coroutines.runBlocking
import org.jetbrains.exposed.sql.Database
import java.nio.file.Path

internal class SchemaDocGenerateCmd : CliktCommand("gene-doc") {

    private val jdbcUrl: String by option("-j", "--jdbc", help = "jdbc url of source db").required()
    private val user: String by option("-u", "--user", help = "username of source db").required()
    private val password: String by option("-p", "--password", help = "password of source db").required()

    private val configFile: Path by option("-c", "--config", help = "path of ingest_config.yaml").path().required()


    override fun run() {
        val config = IngestConfig.fromConfigFile(configFile)
        val sourceDB = Database.connect(url = jdbcUrl, user = user, password = password)
        val schemaDocGenerator = SchemaDocGenerator(config, sourceDB)
        val dispatcher = newFixedThreadPoolContext(30, "worker")

        runBlocking(dispatcher) { schemaDocGenerator.generate() }
    }
}