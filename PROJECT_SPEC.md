# LangChain4j 기반 로컬 RAG & 벡터관리 멀티모듈 설계서 (Java)

> **목표**: 인터넷 연결 없이도 로컬에서 동작하는 RAG(Retrieval-Augmented Generation) 파이프라인과 벡터 인덱스/메타데이터 관리 환경을 **Java / LangChain4j** 기반으로 구축한다. 개발/운영 편의를 위해 **멀티모듈** 구조를 채택하고, 서비스/API, 수집(ingestion), 인덱싱, 질의/재순위, 관측(Observability), 평가(Eval)를 분리한다.

---

## 1) 핵심 요구사항

* **완전 로컬** 또는 **사내망**에서 동작 (모델/벡터DB/메타데이터 스토리지 모두 온프레미스 가능)
* 문서 포맷: PDF, Markdown, HTML, DOCX 등 → 파싱/청킹 후 벡터화 및 색인
* **LangChain4j** 사용 (LLM/임베딩/리트리버, 체인 구성)
* 벡터저장소: **Qdrant**(권장) 또는 **PostgreSQL + pgvector**, (확장안: Milvus)
* 로컬 임베딩/LLM: **Ollama** 기반 (e.g., `nomic-embed-text`, `bge-m3`, LLM은 `llama3.x`, `qwen`, `deepseek` 등)
* **Chunking/메타데이터 관리** 및 **중복 제거**(hash), **버전 관리**, **문서 상태 추적**
* 검색 전략: **Vector-only**, **Hybrid (Vector + BM25)** 선택형
* API: 질의/수집 제어용 **REST**(Spring Boot)
* **Observability**: 인입/출력 로깅, 인퍼런스 지표 기록, 인덱스 상태 대시보드(추가)

---

## 2) 아키텍처 개요

```
+--------------------+       +-------------------+        +-------------------+
|   Client (UI/CLI)  | --->  |  RAG API (REST)   |  --->  |  RAG Orchestrator |
+--------------------+       +-------------------+        +-------------------+
                                      |                               |
                                      v                               v
                             +-----------------+             +-------------------+
                             | Retriever       |<----------->| Re-Ranker (opt.)  |
                             | (LangChain4j)   |             | (Cross-Encoder)   |
                             +-----------------+             +-------------------+
                                      |
                                      v
                        +---------------------------+
                        | Vector Store (Qdrant)     |
                        | + Metadata (Postgres)     |
                        +---------------------------+
                                      ^
                                      |
                        +---------------------------+
                        | Ingestion Pipeline        |
                        | (Tika/PDFBox -> Chunker)  |
                        +---------------------------+
                                      |
                                      v
                               +-----------+
                               |  Ollama   |
                               | (Emb/LLM) |
                               +-----------+
```

* **RAG Orchestrator**: 질의 흐름 제어(질의 -> 리트리브 -> 컨텍스트 구성 -> LLM 호출 -> 답변 생성)
* **Retriever**: kNN(ANN) + 메타 필터, 필요 시 하이브리드(BM25)
* **Vector Store**: Qdrant(콜렉션/필터/점수함수), 메타데이터는 Qdrant payload 또는 별도 RDB
* **Ingestion**: 문서 파싱, 청킹, 임베딩, upsert, 버전/중복 관리
* **Ollama**: 로컬 LLM/Embedding 서버

---

## 3) 기술스택 선택

### 필수

* **Java 21** (LTS)
* **LangChain4j**: LLM/임베딩/리트리버/체인 구성
* **Spring Boot 3.3+**: REST API, 구성/보안/Actuator
* **Qdrant**: 고성능 벡터 데이터베이스 (HNSW/IVF, payload 필터링)
* **Ollama**: 로컬 LLM/임베딩 모델 제공 서버
* **Apache Tika / PDFBox**: 문서 파싱
* **Gradle**(권장) 또는 Maven: 멀티모듈 빌드

### 선택/확장

* **PostgreSQL + pgvector**: 메타데이터 분리/리포팅/조인 필요 시
* **Elasticsearch/OpenSearch**: BM25 하이브리드 검색
* **Redis**: 결과/토큰/캐싱
* **Prometheus + Grafana**: 메트릭/대시보드
* **Keycloak**: 사내 인증/권한 (RBAC)

---

## 4) 멀티모듈 레이아웃 (Gradle)

```
root
├─ build.gradle[.kts]
├─ settings.gradle[.kts]
├─ modules
│  ├─ common               # 공통 유틸/도메인/에러/JSON/DTO
│  ├─ model                # 문서/청크/메타데이터/스키마
│  ├─ embeddings           # 임베딩 엔진 어댑터 (Ollama, HF)
│  ├─ vectorstore          # Qdrant/pgvector 어댑터 (Repository)
│  ├─ retriever            # LangChain4j Retriever 조합, 하이브리드
│  ├─ ingestion            # 파서/청커/중복제거/업서트 파이프라인
│  ├─ rag-core             # Orchestrator/Prompt/Context builder
│  ├─ rag-api              # Spring Boot REST (query/ingest/admin)
│  ├─ re-ranker            # (옵션) Cross-Encoder / Cohere Rerank 대체
│  ├─ eval                 # 간단한 RAG 평가 스위트 (HR@k, nDCG)
│  └─ cli                  # 로컬 운영용 CLI (ingest/run/query)
└─ infra
   ├─ docker-compose.yml   # qdrant/ollama/postgres/elasticsearch 등
   └─ config               # application-*.yml, schema.sql, dashboards
```

> **핵심 포인트**: