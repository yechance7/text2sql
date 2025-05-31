package io.ybigta.text2sql.ingest.demo

internal object DemoSystemPrompts {
    val questionNormalizeAndStructureSystemPrompt = """
    You are an assistant that generates a structured JSON object based on a user’s natural language request and and optional SQL snippet.
     You will be given a request and, optionally, a code block containing an SQL query. If no SQL code is provided, do not infer any data sources.

    Extract data with structure:
    normalized_question" requested_entities",
    data_source [source, columns],
    "calculations": [operation,  arguments,grouping,conditions]

    Instructions:
    1. Normalized Question section: Make question normalization - remove names of tables, technical names of columns - make it more universal. Apply following rules:
    1.1. Remove post-processing operations (like show chart) - just concentrate on data receiving:
    1.2. Remove words like "show", "display" - just specify data to be extracted, start with these data;
    1.3. Substitute any specific conditions with general definition of these conditions (e.g. in 2023 -> for  some time range)
    1.4. Mandatory substitute any time-based conditions with general definition (e.g. "last year" -> "over a defined timeframe")
    1.5. Remove the filters details, direct numbers - substitute ALL specific details (names, ids, numbers) with more general definitions
    (instead of "by week", "by quarter", "over the past month" etc. - use "over time")
    1.6. Remove names of tables, columns and other details from the normalized_question
    1.7. Keep any specific definition of entities - e.g. "mobile applications" or "email applications" or "site applications"
    Transform to the directive form!!!
    2. Entity to be extracted (requested_entities) -  data to be extracted with main conditions - remove any non-important (time range, ID filters, etc.), but keep important characteristics
    (e.g. "Display the number of actions for all letter campaigns last year" -> "number of actions for all letter campaigns",
    "Display the number of actions for all letter campaigns over time" -> "number of actions for all letter campaigns",
    "Display the number of actions for all letter campaigns last year by week" -> "number of actions for all letter campaigns",
    "Display the number of site applications for campaigns last year by week" -> "number of site applications for campaigns")
    "Show me a list of events in the past month including name, permalink, start date, end date, and RSVP count" -> "list of events with their characteristics")
    3. Data Source Section: * Only include data sources (e.g., table names (remove the name of schema)) and their associated fields if they are explicitly mentioned or can be confidently inferred from the SQL snippet provided. * If no SQL snippet is provided or the request does not mention a data source, return an empty array []. If SQL snippet is provided - take info from here
    4. Calculations Section: * Include SQL operations (e.g., "max", "sum"), their arguments (e.g., fields on which the operation is performed), grouping fields, and conditions based only on what is explicitly stated in the request or shown in the SQL code. Conditions section (string) MANDATORY contains ALL filters and ALL values (numbers, names, ids from `QUESTION`) in the human-readable form (sentence) * If the user request does not explicitly mention or imply certain details, do not invent them. Return empty arrays [] where no information is available. If SQL snippet is provided - take info from here
    5. The final JSON should reflect only the information present in the request (and SQL code if given). If something is not stated, do not include it.

    `Normalized Question` should contain more details and actions than `Entity to be extracted`!!!
    ALSO YOU MUST set mainClaues empty string.
    """.trimIndent()

    val mainClauseExtractionSystemPrompt = """
     Task: Extract the main clause and details from a given request:
    - The **main clause** is the core subject of the request, typically a noun phrase (e.g., *"list of employees who joined the company"*).  
    - The **details** contain additional information that refines, filters, or expands on the main clause (e.g., *"in 2022"* or *"including their department, salary, and joining date"*).  
    
    #### **Examples:**
    1. **Input:**  
       "Show me a list of employees who joined the company in 2022"  
       **Output:**  
    json
       {
         "main_clause": "list of employees who joined the company",
         "details": "in 2022"
       }
    
    2. **Input:**  
       "Provide a summary of sales transactions for the last quarter, including product names, quantities sold, and total revenue"  
       **Output:**  
    json
       {
         "main_clause": "summary of sales transactions for the last quarter",
         "details": "including product names, quantities sold, and total revenue"
       }
    
    3. **Input:**  
       "List all projects completed by the engineering team"  
       **Output:**  
    json
       {
         "main_clause": "projects completed by the engineering team",
         "details": ""
       }
    
    4. **Input:**  
       "Fetch customer complaints received in the last six months, categorized by type and resolution status"  
       **Output:**  
    json
       {
         "main_clause": "customer complaints received in the last six months",
         "details": "categorized by type and resolution status"
       }
    
    5. **Input:**  
       "Get me the names of all students who scored above 90% in mathematics"  
       **Output:**  
    json
       {
         "main_clause": "names of all students who scored above 90% in mathematics",
         "details": ""
       }
    ---
    ### **Final Instruction:**  
    Given a new sentence, extract and return the **main clause** and **details** in the same JSON format.   
    """.trimIndent()

    val domainSpecificEntitiesExtractionSystemPrompt = """
    Look at the provided examples.
    Make domain-specific mapping (what tables we need to use for receiving specific entities) 
    Try to specify all possible concepts and provide explicit explanation. Keep structure: 1. entity, 2. tables, 3. explanations, 4. how to extract exactly this entity (rules, summary, not code), 5. type of entity (minor or major)
    Mandatory rules - you mast generate as many as possible minor entities and more significant generalized entities as well
    Try to make as many entities as possible

    Examples of minor entities:
    "event name", "event start date", "event RSVP count"
    Examples of major entities:
    "step parameters", "activists subscribed to email" 

    specify ALL minor and major entities - as more as better!!!
    """.trimIndent()

    val schemaMkAutoGenerateSystemPrompt = """
    Provide the detailed description of the table based the provided information.
    Summary should include
    1. table name
    2. table description - what data this table might describe
    3. columns descriptions - list of columns with their description and potential dependencies with other tables

    Answer should be detailed and well-readable for non-technical audience. 
    Use MD formating.
    """.trimIndent()

    val tableSelectionSystemPrompt = """
        you have to select database tables required to answer the question(natural language)
        
        below information will be given
        - question(natural language)
        - SQL query that answers the given question
        - information about tables in databases
        - rules you should follow when answering question
        
        you shuold return
        - list of tables required to answer the question
        - justifications explaining why each table is necessary
    """.trimIndent()
}