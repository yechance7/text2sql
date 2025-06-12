package io.ybigta.text2sql.ingest.cli

import com.github.ajalt.clikt.core.CliktCommand
import com.github.ajalt.clikt.core.main
import com.github.ajalt.clikt.core.subcommands
import io.ybigta.text2sql.ingest.cli.commands.DomainMappingIngestCmd
import io.ybigta.text2sql.ingest.cli.commands.QaIngestCmd
import io.ybigta.text2sql.ingest.cli.commands.SchemaDocGenerateCmd
import io.ybigta.text2sql.ingest.cli.commands.SchemaIngestCmd

/**
 * nested command를 위한 아무기능도 하지 않는 root place holder command
 */
private class PlaceHolderCmd() : CliktCommand("") {
    override fun run() = Unit
}

fun main(args: Array<String>) = PlaceHolderCmd().subcommands(
    SchemaDocGenerateCmd(),
    SchemaIngestCmd(),
    QaIngestCmd(),
    DomainMappingIngestCmd()
).main(args)