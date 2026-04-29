# Spring AI GoodMem Integration — Test Results

These are the captured outcomes for the integration tests in
`src/test/java/org/springframework/ai/goodmem/GoodMemIntegrationIT.java`. The tests run
against the live GoodMem server configured below and exercise every public tool
surface end-to-end.

## Environment

| Variable | Value |
|---|---|
| `GOODMEM_BASE_URL` | `https://localhost:8080` |
| `GOODMEM_API_KEY`  | `gm_g5xcse2tjgcznlg45c5le4ti5q` (redacted in CI) |
| `GOODMEM_TEST_PDF` | `/home/bashar/Downloads/New Quran.com Search Analysis (Nov 26, 2025)-1.pdf` |
| `GOODMEM_VERIFY_SSL` | `false` (server uses a self-signed cert) |
| Embedder ID used | `019cfd1c-c033-7517-b7de-f73941a0464b` (Voyage AI, auto-selected) |

Live server confirmed reachable via `curl -sk -H "X-API-Key: ..." https://localhost:8080/v1/embedders`,
which returned three embedders (OpenAI Text Embedding 3 Small, Voyage AI, Qwen3 8B).

## Command run

```bash
GOODMEM_BASE_URL=https://localhost:8080 \
GOODMEM_API_KEY=gm_g5xcse2tjgcznlg45c5le4ti5q \
GOODMEM_TEST_PDF="/home/bashar/Downloads/New Quran.com Search Analysis (Nov 26, 2025)-1.pdf" \
GOODMEM_VERIFY_SSL=false \
./mvnw -pl goodmem/spring-ai-goodmem test \
  -Dtest=GoodMemIntegrationIT \
  -DfailIfNoTests=false \
  -Dmaven.build.cache.enabled=false
```

`-Dmaven.build.cache.enabled=false` was required because the repository ships with the
Maven build cache extension, which caches surefire results when source inputs do not
change.

## Outcome

```
[INFO] Tests run: 9, Failures: 0, Errors: 0, Skipped: 0, Time elapsed: 11.89 s
[INFO]  org.springframework.ai.goodmem.GoodMemIntegrationIT
[INFO] BUILD SUCCESS
```

All nine tests passed. Three tests intentionally drive failure paths and the warnings
shown below are the `WARN`-level log lines emitted by `GoodMemTools` after the tool
caught a `GoodMemClientException` and converted it to a clean error map — they are
expected:

```
WARN  GoodMemTools -- goodmem_create_space failed: GoodMem API request failed: HTTP 400 - {"error":"Embedder not found","status":400,"timestamp":...}
WARN  GoodMemTools -- goodmem_retrieve_memories failed: At least one valid Space ID is required.
WARN  GoodMemTools -- goodmem_delete_memory failed: GoodMem DELETE /v1/memories/00000000-0000-0000-0000-000000000000 failed: HTTP 404 - {"error":"Memory not found","status":404,"timestamp":...}
```

## Per-test results

| Order | Test | Outcome | Time | Notes |
|---|---|---|---|---|
| 1 | `listEmbedders_returnsResults` | PASS | 0.010 s | Verified List Embedders tool returns >= 1 embedder |
| 2 | `listSpaces_succeeds` | PASS | 0.015 s | Verified List Spaces tool returns a list |
| 3 | `createSpace_returnsNewSpace` | PASS | 0.030 s | Created a uniquely named space, asserted `reused=false`, deleted afterwards |
| 4 | `createSpace_reusesExistingByName` | PASS | 0.020 s | Verified duplicate-name space returns `reused=true` and the same `spaceId` (no second resource created) |
| 5 | `createMemory_withText_andRetrieve_andGet_andDelete` | PASS | 5.895 s | Full lifecycle: create text memory → retrieve via similarity (results contained the freshly created memoryId) → get memory → delete memory → delete space |
| 6 | `createMemory_withPdf_andRetrieve` | PASS | 5.636 s | Uploaded `New Quran.com Search Analysis (Nov 26, 2025)-1.pdf`, contentType auto-detected as `application/pdf`, retrieve call succeeded |
| 7 | `createSpace_withInvalidEmbedder_returnsActionableError` | PASS | 0.009 s | Invalid embedder UUID surfaces `success=false` with the GoodMem API's `Embedder not found` message preserved (HTTP 400) |
| 8 | `retrieveMemories_withEmptySpaceIds_returnsActionableError` | PASS | 0.001 s | Empty/whitespace `space_ids` returns `success=false` with a clear message: "At least one valid Space ID is required." |
| 9 | `deleteMemory_withInvalidId_returnsActionableError` | PASS | 0.003 s | Unknown memory ID surfaces `success=false` with the GoodMem API's `Memory not found` message preserved (HTTP 404) |

## Coverage of supervisor checklist

* **Create Space** — covered (test 3)
* **Create Memory with text** — covered (test 5)
* **Create Memory with PDF** — covered (test 6, using the supplied PDF path)
* **Retrieve Memories** — covered (tests 5 and 6)
* **Get Memory** — covered (test 5)
* **Delete Memory** — covered (tests 5, 9)
* **List Spaces** — covered (test 2)
* **List Embedders** — covered (test 1)
* **Duplicate space behavior** — covered (test 4): an identical duplicate does NOT
  create a second resource; `reused=true` and the same `spaceId` are returned.
* **Invalid embedder error message** — covered (test 7): the upstream GoodMem error
  text ("Embedder not found") is preserved verbatim in the `error` field, framed in
  Spring AI's expected `success=false`/`error`/`statusCode` shape.
* **Invalid memory ID error message** — covered (test 9): "Memory not found" preserved
  verbatim with HTTP 404.
* **Empty/invalid space IDs** — covered (test 8): client-side validation catches the
  problem before any HTTP call.
* **PDF really used** — covered (test 6): the file path is asserted to exist and the
  resulting memory's `contentType` is asserted to be `application/pdf`.

## Items intentionally NOT exercised by the live IT

* **Conflicting-duplicate space (same name, different embedder):** the GoodMem
  client's dedupe path is name-only, matching the langgraph reference. When a caller
  provides a different `embedderId` on the second create, the existing space is reused
  and the **old embedder remains in effect** (the new `embedderId` is recorded in the
  result map but is not applied to the existing space). This is a deliberate behavior
  inherited from the reference integration.
* **Duplicate embedder creation:** spring-ai does not surface embedder creation as a
  tool — only `list_embedders` is exposed (matching the reference). Marked
  `NOT APPLICABLE`.
* **Unsupported reranker / LLM IDs:** can be exercised by setting `rerankerId`/`llmId`
  to a bogus UUID against the live server; the behavior would mirror test 7 (clean
  `success=false`/`error` propagation). Not added to the IT to keep the suite focused
  on the four required scenarios plus the reference parity behaviors.

## Additional unit tests

```
[INFO] Running org.springframework.ai.goodmem.GoodMemToolsTests
[INFO] Tests run: 2, Failures: 0, Errors: 0, Skipped: 0
```

`GoodMemToolsTests.allExpectedToolsAreAnnotated` asserts that exactly the seven
expected `@Tool` names are present (no missing or duplicated tools).
`GoodMemToolsTests.allToolsHaveDescriptions` asserts every tool has a non-blank
description so the AI model receives meaningful tool metadata.
