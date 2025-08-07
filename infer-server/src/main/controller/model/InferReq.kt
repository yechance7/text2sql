// InferReq.kt
package io.ybigta.text2sql.infer.server.controller.model

import kotlinx.serialization.Serializable

@Serializable
internal data class InferReq(
    val userId: String,
    val question: String
)