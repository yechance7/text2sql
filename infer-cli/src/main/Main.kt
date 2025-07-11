package io.ybigta.text2sql.infer.cli

import com.github.ajalt.clikt.core.CliktCommand
import com.github.ajalt.clikt.core.main
import com.github.ajalt.clikt.core.subcommands
import io.ybigta.text2sql.infer.cli.commands.BatchInferCmd
import io.ybigta.text2sql.infer.cli.commands.EvalTableRetrieveCmd
import io.ybigta.text2sql.infer.cli.commands.InferCmd

/**
 * nested command를 위한 아무기능도 하지 않는 root place holder command
 */
private class PlaceHolderCmd() : CliktCommand("") {
    override fun run() = Unit
}

fun main(args: Array<String>) =
    PlaceHolderCmd()
        .subcommands(
            InferCmd(),
            BatchInferCmd(),
            EvalTableRetrieveCmd()
        )
        .main(args)