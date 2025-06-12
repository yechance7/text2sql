package io.ybigta.text2sql.ingest.llmendpoint

internal interface QuestionMainClauseExtractionEndpoint {
    /**
     * request llm to extract main clauses from give natural language question
     */
    fun request(question: String): String
}