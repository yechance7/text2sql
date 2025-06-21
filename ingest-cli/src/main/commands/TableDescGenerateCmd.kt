package io.ybigta.text2sql.ingest.cli.commands

import com.charleskorn.kaml.MultiLineStringStyle
import com.charleskorn.kaml.SingleLineStringStyle
import com.charleskorn.kaml.Yaml
import com.charleskorn.kaml.YamlConfiguration
import com.github.ajalt.clikt.core.CliktCommand
import com.github.ajalt.clikt.parameters.options.default
import com.github.ajalt.clikt.parameters.options.option
import com.github.ajalt.clikt.parameters.options.required
import com.github.ajalt.clikt.parameters.types.enum
import com.github.ajalt.clikt.parameters.types.path
import io.ybigta.text2sql.ingest.TableDesc
import io.ybigta.text2sql.ingest.TableDescGenerator
import io.ybigta.text2sql.ingest.TableName
import io.ybigta.text2sql.ingest.config.IngestConfig
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.newFixedThreadPoolContext
import kotlinx.coroutines.runBlocking
import kotlinx.serialization.StringFormat
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import org.jetbrains.exposed.sql.Database
import org.slf4j.LoggerFactory
import java.nio.file.Path
import kotlin.io.path.createDirectories
import kotlin.io.path.exists
import kotlin.io.path.moveTo
import kotlin.io.path.writeText

internal class TableDescGenerateCmd : CliktCommand("gene-desc") {

    private val jdbcUrl: String by option("-j", "--jdbc", help = "jdbc url of source db").required()
    private val user: String by option("-u", "--user", help = "username of source db").required()
    private val password: String by option("-p", "--password", help = "password of source db").required()

    private val configFile: Path by option("-c", "--config", help = "path of ingest_config.yaml").path().required()

    private val tableDescFileType by option("-t", "--type", help = "file type of table-desc")
        .enum<TableDescFileType> { it.name.lowercase() }
        .default(TableDescFileType.YAML)

    private val logger = LoggerFactory.getLogger(this::class.java)

    private val prettyJson = Json { prettyPrint = true }
    private val prettyYaml = Yaml(
        configuration = YamlConfiguration(
            singleLineStringStyle = SingleLineStringStyle.Plain,
            multiLineStringStyle = MultiLineStringStyle.Literal
        )
    )

    override fun run() {
        val config = IngestConfig.fromConfigFile(configFile)
        val sourceDB = Database.connect(url = jdbcUrl, user = user, password = password)
        val tableDescGenerator = TableDescGenerator.fromConfig(config, sourceDB)

        val saveBaseDir = config.config.resources.schemaMarkdownDir.toAbsolutePath().normalize().also { it.createDirectories() }

        val dispatcher = newFixedThreadPoolContext(30, "worker")

        runBlocking(dispatcher) {
            tableDescGenerator
                .generateTableDesc()
                .saveTableDescToFile(saveBaseDir, tableDescFileType)
        }
    }

    private suspend fun Flow<Pair<TableName, TableDesc>>.saveTableDescToFile(saveBaseDir: Path, fileType: TableDescFileType) = this.collect { (tableName, tableDesc) ->
        val encoder: StringFormat = when (fileType) {
            TableDescFileType.JSON -> prettyJson
            TableDescFileType.YAML -> prettyYaml
        }
        val savePath = saveBaseDir.resolve("${tableName.schemaName}.${tableName.tableName}.${fileType.prefix}")

        if (savePath.exists()) {
            val movePath = saveBaseDir.resolve("${tableName.schemaName}.${tableName.tableName}.${System.currentTimeMillis()}.${fileType.prefix}")
            savePath.moveTo(movePath)
            logger.warn("moved pre-existing ${savePath} to ${movePath}")
        }

        encoder
            .encodeToString(tableDesc)
            .let { savePath.writeText(it) }

        logger.info("written schema markdown document ${savePath}")
    }


}

internal enum class TableDescFileType(val prefix: String) {
    JSON("json"), YAML("yml")
}


