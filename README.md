# what is this

This is kotlin implementation of [datrics-ai/text2sql](https://github.com/datrics-ai/text2sql).

Also project is part of industrial-educational cooperation of [DTOL](https://dtol.co.kr/main) and [ybigta](https://github.com/ybigta)

# overall explaination about how it works

`datrics-ai/text2sql` use RAG&LLM approach for text2sql.
Which means we need to insert data like *table schema*, *qustion-sql-pair*, *domain-knowldege* to vector_db first(`ingest`) then request LLM with giving her retrieved data(`infer`).

For more detail [HOW-IT-WORKS](./docs/HOW-IT-WORKS.md)

--- 

# how to build ingest cli

- requirements(based on my desktop environment)
    - maven (3.9.9)
    - kotlin (2.1.10)
    - jvm (1.8 ~ 23)

If you didn't installed above them. I recommend you to install them by [sdkman](https://sdkman.io)

below command will create `ingest-cli-<version>.jar` executable jar.

```bash
git clone https://github.com/jsybf/text2sql.git
cd text2sql
RUN mvn clean package -pl ingest-cli -am
```

---

# how to build infer server

- requirements(based on my desktop environment)
    - maven (3.9.9)
    - kotlin (2.1.10)
    - jvm (1.8 ~ 23)

```bash
git clone https://github.com/jsybf/text2sql.git
cd text2sql
RUN mvn clean package -pl infer-server -am
```

build infer server via docker is also another option.

```bash
git clone https://github.com/jsybf/text2sql.git
cd text2sql
docker build \
      --build-arg config_path=<path of infer_config.yaml> \
      -f infer-server/Dockerfile \
      -t ybigta-dtol/infer-server \ # what ever
      .
      
docker run -it --rm  -p 8080:8080 ybigta-dtol/infer-server

```

request example

```bash
 curl -X POST \
        -H "Content-Type: text/plain" \
        -d "개선대책담당자중 가장 많은 개선대책을 담당한 사람의 이름을 찾아줘" \
        http://localhost:8080/infer
```

---

# usage(ingest)

- requirements
    - jvm (1.8 ~ 23)
    - [config yaml file](ingest_config.yaml.example)
    - postgres with pgvector extensions

first you need to prepare config file. [config file template](ingest_config.example.yaml) is prepared.
not modifing `systemPrompt` fields is suggested

```


### schema doc generation
read tables from database and generate json description.

```bash
java -jar ingest-cli-<version>.jar gene-desc \
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

```bash
java -jar ingest-cli-<version>.jar  ingest-qa --config <CONFIG_FILE_PATH>
```

### domain mapping ingest

ingest domain mapping from QA in vectordb.
require `ingest-qa`, `ingest-schema` called before

```bash
java -jar ingest-cli-<version>.jar  ingest-domain-mapping --config <CONFIG_FILE_PATH>
```
