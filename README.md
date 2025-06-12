# what is this

this is kotlin implementation of [text2sql](https://github.com/datrics-ai/text2sql).

# build

- requirements(based on my desktop environment)
    - maven (3.9.9)
    - kotlin (2.1.10)
    - jvm (1.8 ~ 23)

if you didn't installed above them. i recommend you to install them by [sdkman](https://sdkman.io)

this command will create `ingest-cli-<version>.jar` executable file.


```bash
git clone https://github.com/jsybf/text2sql.git
mvn package
```

---

# usage(ingest)

- requirements
    - jvm (1.8 ~ 23)
    - [config yaml file](ingest_config.yaml.example)

first you need to prepare config file. [config file template](ingest_config.yaml.example) is prepared.
not modifing `systemPrompt` fields is suggested
```yaml
Resources:
  qaJson: ./data/qa.json # path of QA-pair json file
  schemaMarkdownDir: ./data/desc # path of dir to read/write table schema markdown doc

# config vectordb
PgVector:
  jdbcUrl: jdbc:postgresql://localhost:5432/vectordb # jdbc url of postgres(pgvector extension should be installed)
  userName: ???
  password: ???
  
# list of llm models
# currently only open ai model that supports json output is supported
LLMModels:
  - modelName: gpt-4o-mini 
    apiKey: ???
  - modelName: gpt-4.1-mini
    apiKey: ???
    
# currently only open ai model is only supported
EmbeddingModel:
  modelName: text-embedding-ada-002 # vector size should be sync with postgres pgvector size
  apiKey: ???
  
# specify llm-model and systemprompt for each llm call
LLMEndPoints:
  SchemaMarkdownGeneration:
    SchemaMarkdownGenerationEndpoint:
      modelName: gpt-4o-mini // one of modelName specified in LLMModels
      systemPrompt: ???
  SchemaIngest:
    StrucutureSchemaDocEndpoint:
      modelName: gpt-4.1-mini
      systemPrompt: ???
    TableEntitiesExtractionEndpoint:
      modelName: gpt-4.1-mini
      systemPrompt: ???
  QaIngest:
    QuestionNormalizeAndStructureEndpoint:
      modelName: gpt-4.1-mini
      systemPrompt:

```


### schema doc generation
read tables from database and generate table schema documentation(markdown).
modifing and validating auto-generated documentation is highly recommended

```bash
java -jar ingest-cli-<version>.jar gene-doc \
  --config <CONFIG_FILE_PATH> \
  --jdbc <JDBC_URL> \
  --user <DB_USER> \
  --password <DB_PW>
```

### schema ingest

ingest table schema(markdown) to vectordb

```bash

java -jar ingest-cli-<version>.jar ingest-schema --config <CONFIG_FILE_PATH>
```

### qa ingest

ingest QA(question: natural langauge, answer: SQL)json file to vectordb
require `ingest-schema` called before

```bash
java -jar ingest-cli-<version>.jar  ingest-qa --config <CONFIG_FILE_PATH>
```

### domain mapping ingest

ingest domain mapping from QA in vectordb.
require `ingest-qa` called before

```bash
java -jar ingest-cli-<version>.jar  ingest-domain-mapping --config <CONFIG_FILE_PATH>
```
