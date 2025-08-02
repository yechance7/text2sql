package io.ybigta.text2sql.infer.server.route.model

import kotlinx.serialization.Serializable

@Serializable
internal data class InferReq(
    val question: String
)