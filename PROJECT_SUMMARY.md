# LangChain4j RAG 시스템 구축 완료 보고서

## 📋 프로젝트 개요
- **프로젝트명**: LangChain4j 기반 로컬 RAG & 벡터관리 멀티모듈 시스템
- **목표**: 인터넷 연결 없이 로컬에서 동작하는 RAG 파이프라인 구축
- **기술스택**: Java 21, Spring Boot 3.3.4, LangChain4j, Elasticsearch, Ollama
- **구조**: Gradle 멀티모듈 아키텍처

## 🏗️ 구축된 시스템 아키텍처

### 모듈 구조
```
ds-ai2-rag/
├── modules/
│   ├── common/          # 공통 유틸리티, DTO, 예외처리
│   ├── model/           # 도메인 모델 (Document, Chunk, QueryRequest)
│   ├── embeddings/      # Ollama 임베딩 서비스
│   ├── vectorstore/     # Elasticsearch 벡터 저장소
│   ├── ingestion/       # 문서 수집 및 청킹 서비스
│   ├── rag-core/        # RAG 오케스트레이터
│   └── rag-api/         # Spring Boot REST API
├── docker-compose.yml   # Docker 서비스 구성
├── build.gradle.kts     # 루트 빌드 설정
└── settings.gradle.kts  # 멀티모듈 설정
```

### Docker 서비스 구성
- **Elasticsearch** (포트 9200): 벡터 저장소 및 하이브리드 검색
- **Kibana** (포트 5601): Elasticsearch 모니터링
- **Ollama** (포트 11434): 로컬 LLM 및 임베딩 모델
- **PostgreSQL** (포트 5432): 메타데이터 저장소
- **Redis** (포트 6379): 캐싱 레이어

## ✅ 구현된 주요 기능

### 1. 문서 처리 (Ingestion)
- **DocumentIngestionService**: 문서 파싱 및 처리
- **ChunkingService**: 문서를 청크로 분할
- 지원 형식: PDF, Markdown, HTML, DOCX, TXT, JSON, XML
- Apache Tika를 통한 자동 문서 파싱
- MD5 해시 기반 중복 제거

### 2. 임베딩 생성 (Embeddings)
- **EmbeddingService**: Ollama 통합 임베딩 서비스
- 지원 모델: nomic-embed-text, bge-m3
- 배치 임베딩 처리 지원
- TextSegment 변환 및 벡터 생성

### 3. 벡터 저장소 (VectorStore)
- **ElasticsearchVectorStore**: Elasticsearch 기반 벡터 저장
- 벡터 검색 (Script Query 사용)
- 하이브리드 검색 (Vector + BM25)
- 문서별 삭제 및 관리 기능

### 4. RAG 오케스트레이션 (Core)
- **RagOrchestrator**: 질의 처리 파이프라인
- 검색 전략: VECTOR_ONLY, HYBRID, BM25_ONLY
- 컨텍스트 구성 및 프롬프트 생성
- Ollama LLM 통합 (llama3.2)

### 5. REST API
- **QueryController**: RAG 질의 엔드포인트
- **IngestionController**: 문서 업로드/삭제
- Swagger UI 통합
- Spring Boot Actuator 헬스체크

## 🔧 주요 설정 및 구성

### application.yml
```yaml
# Ollama 설정
ollama:
  base-url: http://localhost:11434
  embedding-model: nomic-embed-text
  chat-model: llama3.2
  timeout: 120

# Elasticsearch 설정
elasticsearch:
  host: localhost
  port: 9200
  index: rag-chunks

# 청킹 설정
ingestion:
  chunk-size: 500
  chunk-overlap: 100
  batch-size: 10
```

### 의존성 버전
- LangChain4j: 0.35.0
- Spring Boot: 3.3.4
- Elasticsearch: 8.11.1
- Ollama: 0.35.0
- Apache Tika: 2.9.1
- PDFBox: 3.0.1

## 🚀 실행 방법

### 1. Docker 서비스 시작
```bash
docker-compose up -d
```

### 2. 애플리케이션 실행
```bash
# Gradle로 실행
./gradlew :modules:rag-api:bootRun

# 또는 IntelliJ에서
# RagApplication.java 우클릭 → Run
```

### 3. API 테스트
```bash
# 문서 업로드
curl -X POST "http://localhost:8080/api/v1/ingest/file?path=/path/to/document.md"

# RAG 질의
curl -X POST "http://localhost:8080/api/v1/query" \
  -H "Content-Type: application/json" \
  -d '{
    "query": "질문 내용",
    "topK": 5,
    "strategy": "HYBRID"
  }'
```

## 📊 테스트 결과

### 성공 사항
- ✅ 멀티모듈 프로젝트 구조 구축
- ✅ Docker 서비스 정상 실행
- ✅ Spring Boot 애플리케이션 구동
- ✅ 문서 업로드 및 청킹
- ✅ Elasticsearch 인덱싱

### 발견된 이슈 및 해결방안
1. **Ollama IPv6 연결 문제**
   - 원인: localhost가 IPv6로 해석되어 연결 실패
   - 해결: application.yml에서 `127.0.0.1` 사용

2. **Elasticsearch 벡터 검색 호환성**
   - 원인: Elasticsearch 8.11 API 변경
   - 해결: Script Query 기반 코사인 유사도 검색 구현

## 📝 향후 개선사항

### 기능 개선
- [ ] Qdrant 벡터 DB 통합 (더 나은 벡터 검색)
- [ ] 재순위화(Re-ranking) 모듈 구현
- [ ] 평가(Evaluation) 메트릭 추가
- [ ] CLI 도구 개발

### 성능 최적화
- [ ] 임베딩 캐싱 구현
- [ ] 비동기 처리 도입
- [ ] 배치 처리 최적화

### 운영 기능
- [ ] Prometheus + Grafana 모니터링
- [ ] 로깅 및 추적 개선
- [ ] API 인증/권한 관리
- [ ] 문서 버전 관리

## 🎯 프로젝트 성과
- 완전 로컬 환경에서 동작하는 RAG 시스템 구축
- 모듈화된 클린 아키텍처 구현
- Spring Boot와 LangChain4j 통합 성공
- Docker 기반 개발 환경 구성
- RESTful API 및 Swagger 문서화

## 📚 참고 자료
- [LangChain4j 공식 문서](https://docs.langchain4j.dev/)
- [Elasticsearch Java Client](https://www.elastic.co/guide/en/elasticsearch/client/java-api-client/current/index.html)
- [Ollama API](https://github.com/ollama/ollama/blob/main/docs/api.md)
- [Spring Boot 3.3 문서](https://docs.spring.io/spring-boot/docs/3.3.x/reference/html/)

---

**작성일**: 2025년 9월 14일  
**작성자**: Claude Code Assistant  
**프로젝트 위치**: `/Users/ijeongseob/IdeaProjects/ds-ai2`