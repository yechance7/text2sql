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

# usage

- requirements
    - jvm (1.8 ~ 23)
    - [config yaml file](ingest_config.yaml.example)

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
