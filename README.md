# LangChain4j 기반 로컬 RAG 시스템

Java 21과 LangChain4j를 기반으로 구축된 로컬 RAG(Retrieval-Augmented Generation) 시스템입니다.

## 주요 기능

- 🚀 **완전 로컬 실행**: 인터넷 연결 없이 온프레미스 환경에서 동작
- 📄 **다양한 문서 지원**: PDF, Markdown, HTML, DOCX, TXT 등
- 🔍 **하이브리드 검색**: Vector Search + BM25 텍스트 검색
- 🤖 **Ollama 통합**: 로컬 LLM 및 임베딩 모델 지원
- 📊 **Elasticsearch**: 확장 가능한 벡터 저장소 및 검색 엔진
- 🏗️ **멀티모듈 구조**: 깔끔하고 유지보수가 쉬운 아키텍처

## 시스템 요구사항

- Java 21+
- Docker & Docker Compose
- 16GB+ RAM (권장)
- 50GB+ 디스크 공간

## 빠른 시작

### 1. 프로젝트 클론 및 환경 설정

```bash
# 환경 변수 설정
cp .env.example .env

# Docker 서비스 시작
docker-compose up -d
```

### 2. 애플리케이션 빌드 및 실행

```bash
# Gradle 빌드
./gradlew clean build

# Spring Boot 애플리케이션 실행
./gradlew :modules:rag-api:bootRun
```

### 3. API 접속

- Swagger UI: http://localhost:8080/swagger-ui.html
- API Docs: http://localhost:8080/api-docs
- Health Check: http://localhost:8080/actuator/health

## 프로젝트 구조

```
.
├── modules/
│   ├── common/          # 공통 유틸리티 및 DTO
│   ├── model/           # 도메인 모델
│   ├── embeddings/      # 임베딩 서비스 (Ollama)
│   ├── vectorstore/     # 벡터 저장소 (Elasticsearch)
│   ├── ingestion/       # 문서 수집 및 처리
│   ├── rag-core/        # RAG 오케스트레이션
│   └── rag-api/         # REST API
├── docker-compose.yml   # Docker 구성
└── README.md

```

## Docker 서비스

| 서비스 | 포트 | 설명 |
|--------|------|------|
| Elasticsearch | 9200 | 벡터 저장소 및 검색 엔진 |
| Kibana | 5601 | Elasticsearch 모니터링 |
| Ollama | 11434 | 로컬 LLM/임베딩 서버 |
| PostgreSQL | 5432 | 메타데이터 저장소 |
| Redis | 6379 | 캐싱 레이어 |

## API 사용 예제

### 문서 업로드

```bash
curl -X POST http://localhost:8080/api/v1/ingest/upload \
  -F "file=@document.pdf"
```

### 질의 실행

```bash
curl -X POST http://localhost:8080/api/v1/query \
  -H "Content-Type: application/json" \
  -d '{
    "query": "LangChain4j의 주요 기능은?",
    "topK": 5,
    "strategy": "HYBRID"
  }'
```

## 모델 설정

Ollama 모델 다운로드:

```bash
# 임베딩 모델
docker exec rag-ollama ollama pull nomic-embed-text
docker exec rag-ollama ollama pull bge-m3

# LLM 모델
docker exec rag-ollama ollama pull llama3.2
docker exec rag-ollama ollama pull qwen2.5
```

## 개발 가이드

### 모듈별 빌드

```bash
# 특정 모듈만 빌드
./gradlew :modules:rag-api:build

# 테스트 실행
./gradlew test

# 통합 테스트
./gradlew integrationTest
```

### 로컬 개발 환경

1. IntelliJ IDEA 또는 Eclipse에서 프로젝트 열기
2. Lombok 플러그인 설치
3. Gradle 프로젝트로 임포트
4. `RagApplication.java` 실행

## 설정 옵션

`application.yml`에서 다음 설정을 조정할 수 있습니다:

```yaml
# Ollama 설정
ollama:
  base-url: http://localhost:11434
  embedding-model: nomic-embed-text
  chat-model: llama3.2

# Elasticsearch 설정
elasticsearch:
  host: localhost
  port: 9200
  index: rag-chunks

# 청킹 설정
ingestion:
  chunk-size: 500
  chunk-overlap: 100
```

## 트러블슈팅

### Elasticsearch 연결 오류
```bash
# Elasticsearch 상태 확인
curl http://localhost:9200/_cluster/health
```

### Ollama 모델 오류
```bash
# Ollama 로그 확인
docker logs rag-ollama

# 모델 목록 확인
docker exec rag-ollama ollama list
```

### 메모리 부족
Docker Desktop 설정에서 메모리를 8GB 이상으로 할당하세요.

## 라이선스

MIT License

## 기여

Pull Request와 Issue는 언제나 환영합니다!

## 문의

프로젝트 관련 문의사항은 Issue를 통해 남겨주세요.