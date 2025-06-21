RAG&LLM을 통한 text2sql에서 본질은 LLM에게 보낼요청을 잘 생성해내는 것이다.

# 적제과정

우선 vectordb(postgres with pgvector)에 어떤 데이터를 어떻게 저장하는 지부터 알아보자.

우선 개괄적인 과정은 아래와 같다

![image1.svg](images/image1.svg)

- 1: 디비에서 테이블 스키마정보를 긁어온뒤 자동으로 json문서를 생성. 이문서느 검토후 수정하는 걸 강력히 추천
- 2: 자연어-sql 예시쌍을 저장
- 3: 특정 엔티티를 찾기 위해 필요한 테이블들을 추론

`table_schema`는 이런 형태로 저장된다.
```json
{
  "keys": "Composite primary = co_id, biz_cd, biz_pic_id\n",
  "columns": [
    {
      "column": "co_id",
      "description": "Stores the unique identifier for a company; part of the primary key and may reference the Company Master table."
    },
    {
      "column": "biz_cd",
      "description": "Represents the business task type code; part of the primary key and likely linked to a Business Codes table."
    },
    {
      "column": "biz_pic_id",
      "description": "Identifier of the business person in charge; part of the primary key and may correspond to a Personnel table."
    },
    {
      "column": "frst_regr_id",
      "description": "ID of the user who first registered the record for auditing purposes."
    },
    {
      "column": "frst_reg_dtm",
      "description": "Timestamp when the record was first created, used for tracking data provenance."
    },
    {
      "column": "last_mdfr_id",
      "description": "ID of the user who last modified the record, aiding in audit tracking."
    },
    {
      "column": "last_mdf_dtm",
      "description": "Timestamp indicating the last time the record was updated."
    }
  ],
  "purpose": "~~~",
  "summary": "~~~",
  "tableName": {
    "tableName": "cm_co_biz_pic",
    "schemaName": "comn"
  },
  "weakEntities": [
    "business personnel",
    "company business",
    "audit tracking",
    "business task",
    "business person",
    "business function",
    "business code"
  ],
  "strongEntities": [
    "business personnel",
    "audit tracking",
    "business task",
    "business person"
  ],
  "connectedTables": [
    {
      "tableName": "cm_co",
      "schemaName": "comn"
    }
  ],
  "dependenciesThought": "```DBML\ncomn.cm_co_biz_pic.co_id > comn.cm_co.co_id\n```\n"
}
```

`qa`쌍은 이런 형태로 변환이 되서 저장이 된다.
```json
{
  "question": "회사별 검사데이터 건수를 알려줘, 그리고 건수가 가장 많은 순서로 출력해줘",
  "dataSource": [
    {
      "table": "qm_vndr_qlty_data",
      "columns": [
        "vndr_id"
      ]
    },
    {
      "table": "cm_co",
      "columns": [
        "co_id",
        "co_nm"
      ]
    }
  ],
  "mainClause": "```json\n{\n  \"main_clause\": \"회사별 검사데이터 건수\",\n  \"details\": \"건수가 가장 많은 순서로 출력\"\n}\n```",
  "calculations": [
    {
      "grouping": [
        "vndr_id",
        "co_nm"
      ],
      "arguments": [
        "1"
      ],
      "operation": "count",
      "conditions": "join condition where vendor ID equals company ID"
    }
  ],
  "requestedEntities": "count of inspection data by company",
  "normalizedQuestion": "Extract the count of inspection data grouped by companies and sort them in descending order by count."
}
```

`domain_entity_mapping`은 이런 형태로 저장이 된다. 예는 따로 임베딩을 하지 않고 LLM에게 보낼 요청을 만들때 사용된다.
```json
{
  "entityName": "customer company relation ID",
  "sourceTables": [
    "comn.cm_co"
  ],
  "classification": "minor",
  "entityConceptualRole": "key to join company and field claim data"
}
```

# 추론과정

실제로 text를 sql로 변환하는 작업은 LLM이 수행.

우리가 해야하는 것은 LLM에게 보낼 요청을 잘 생성해내는 것

유저의 질문을 바탕으로 QA쌍과 table 정보를 검색해 요청을 만들어내고 이를 LLM에게 전달해준다.

![image2.svg](images/image2.svg)

