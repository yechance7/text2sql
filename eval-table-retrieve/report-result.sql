-- how to use:
-- cat table-retrieve-output-2.json | duckdb -f ./eval-table-retrieve/anal-result.sql


CREATE TABLE eval(
	question        VARCHAR,
	answerTables 	VARCHAR[],
	retrievedTables JSON
);

COPY eval FROM '/dev/stdin'(FORMAT json, AUTO_DETECT true, ARRAY true);

WITH 
	step1 AS (
		SELECT 
			answerTables,
			unnest(retrievedTables::JSON[]) AS retrievedTable
		FROM eval
	),
	step2 AS (
		SELECT 
			retrievedTable->>'$.table' as table,
			retrievedTable->>'$.embeddingCategory' as category,
			retrievedTable->>'$.distance' as distance,
			answerTables
		FROM step1
		WHERE retrievedTable->>'$.table' in  answerTables
	),
	step3 AS (
		SELECT category, COUNT(1)
		FROM step2
		GROUP BY category
	),
	step31 AS (
		SELECT category, ROUND(AVG(CAST(distance AS FLOAT)), 4) as dist_avg, COUNT(1) as cnt
		FROM step2
		GROUP BY category
	)
select * from step31
;


WITH 
	step1 AS (
		SELECT 
			answerTables as aTbls,
			retrievedTables->>'$[*].table' as pTbls
		FROM eval
	),
	step2 AS (
		SELECT 
			aTbls,
			pTbls,
			CASE
				WHEN aTbls = pTbls THEN '정확히 일치'
				WHEN (NOT list_has_all(pTbls, aTbls)) AND (list_has_all(aTbls, pTbls)) THEN '필요한 걸 못찾음'
				WHEN (list_has_all(pTbls, aTbls)) AND (NOT list_has_all(aTbls, pTbls)) THEN '불필요한것도 찾음'
				WHEN (NOT list_has_all(pTbls, aTbls)) AND (NOT list_has_all(aTbls, pTbls)) THEN '필요한 걸 못찾음 && 불필요한것도 찾음'
			END AS result
		FROM step1
	)
SELECT 
	result,
	COUNT(1) as cnt,
	ROUND(COUNT(1) * 100.0 / SUM(COUNT(1)) OVER(), 2) as percentage
FROM step2
GROUP BY result 
ORDER BY cnt DESC
;
