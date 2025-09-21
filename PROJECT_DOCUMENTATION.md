# DS-AI2 RAG 프로젝트 문서

## 📋 프로젝트 개요

**DS-AI2**는 RAG(Retrieval-Augmented Generation) 기반의 AI 채팅 애플리케이션입니다. Elasticsearch를 벡터 스토어로 사용하고, Ollama를 통해 LLM 모델과 통합되어 있습니다.

### 주요 기능
- **RAG 모드**: 문서 검색 기반 AI 답변 생성
- **Direct Ollama 모드**: LLM 모델에 직접 질의
- **일반 채팅 모드**: 세션 메모리를 활용한 대화
- **문서 인제스천**: PDF, TXT, DOCX 등 다양한 문서 형식 지원
- **벡터 검색**: Elasticsearch를 활용한 의미 기반 검색

## 🏗️ 아키텍처

### 기술 스택
- **Backend**: Spring Boot 3.3.4, Java 21
- **LLM Integration**: Ollama (원격 서버: https://nerget-open-webui-ollama-int.dev.nerget.co.kr)
- **Vector Store**: Elasticsearch
- **Database**: MySQL 8.0
- **Embedding Model**: nomic-embed-text
- **Chat Model**: llama3.2-vision:11b
- **Build Tool**: Gradle (Multi-module)

### 모듈 구조

```
ds-ai2/
├── modules/
│   ├── common/          # 공통 유틸리티 및 DTO
│   ├── model/           # 데이터 모델 및 엔티티
│   ├── embeddings/      # 임베딩 서비스
│   ├── vectorstore/     # Elasticsearch 벡터 스토어
│   ├── ingestion/       # 문서 처리 및 청킹
│   ├── rag-core/        # 핵심 RAG 로직 및 서비스
│   └── rag-api/         # REST API 및 웹 UI
```

## 🚀 설치 및 실행

### 사전 요구사항
1. **Java 21** 설치
2. **MySQL 8.0** 실행
3. **Elasticsearch** 실행 (포트: 9200)
4. **Ollama 서버** 접근 가능

### 데이터베이스 설정

```sql
CREATE DATABASE js DEFAULT CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;
CREATE USER 'nerget-user'@'localhost' IDENTIFIED BY 'nerget';
GRANT ALL PRIVILEGES ON js.* TO 'nerget-user'@'localhost';
FLUSH PRIVILEGES;
```

### 빌드 및 실행

```bash
# 프로젝트 클론
git clone [repository-url]
cd ds-ai2

# 빌드
./gradlew clean build -x test

# 실행
java -jar modules/rag-api/build/libs/rag-api-1.0.0-SNAPSHOT.jar

# 또는 한 번에
./gradlew :modules:rag-api:bootRun
```

### 환경 변수 설정 (선택사항)

```bash
export OLLAMA_BASE_URL=https://nerget-open-webui-ollama-int.dev.nerget.co.kr
export OLLAMA_EMBEDDING_MODEL=nomic-embed-text
export OLLAMA_CHAT_MODEL=llama3.2-vision:11b
export ES_HOST=127.0.0.1
export ES_PORT=9200
```

## 📝 주요 설정 파일

### application.yml
`modules/rag-api/src/main/resources/application.yml`

```yaml
# Ollama 설정
ollama:
  base-url: https://nerget-open-webui-ollama-int.dev.nerget.co.kr
  embedding-model: nomic-embed-text
  chat-model: llama3.2-vision:11b
  timeout: 120

# Elasticsearch 설정
elasticsearch:
  host: 127.0.0.1
  port: 9200
  index: rag-chunks

# MySQL 설정
spring:
  datasource:
    url: jdbc:mysql://localhost:3306/js
    username: nerget-user
    password: nerget
```

## 🔌 API 엔드포인트

### 채팅 API

#### 1. 일반 채팅 (RAG 옵션 포함)
```http
POST /api/v1/chat
Content-Type: application/json

{
  "sessionId": "uuid",
  "message": "질문 내용",
  "useRag": true/false
}
```

#### 2. Direct Ollama 채팅
```http
POST /api/v1/direct-chat
Content-Type: application/json

{
  "message": "질문 내용",
  "model": "llama3.2-vision:11b"  // 선택사항
}
```

#### 3. 새 세션 생성
```http
POST /api/v1/chat/new
```

#### 4. 사용 가능한 모델 조회
```http
GET /api/v1/direct-chat/models
```

### 문서 관리 API

#### 1. 문서 업로드
```http
POST /api/v1/documents/upload
Content-Type: multipart/form-data

file: [파일]
```

#### 2. 문서 검색
```http
POST /api/v1/rag/search
Content-Type: application/json

{
  "query": "검색어",
  "topK": 5
}
```

## 🖥️ 웹 UI

브라우저에서 `http://localhost:8080` 접속

### UI 기능
- **RAG 모드 토글**: 문서 검색 기반 답변 활성화/비활성화
- **Direct Ollama 모드**: LLM 모델 직접 호출 모드
- **모델 선택**: Direct 모드에서 사용할 모델 선택
- **세션 관리**: 새 대화 시작 및 세션 ID 표시
- **실시간 타이핑 인디케이터**: AI 응답 대기 중 표시

## 🛠️ 개발 가이드

### 새 모듈 추가
```groovy
// settings.gradle.kts
include(":modules:new-module")

// modules/new-module/build.gradle.kts
dependencies {
    implementation(project(":modules:common"))
    // 기타 의존성
}
```

### 새 API 엔드포인트 추가
```java
@RestController
@RequestMapping("/api/v1/new-endpoint")
public class NewController {
    @PostMapping
    public ResponseEntity<BaseResponse<Result>> newMethod(@RequestBody Request request) {
        // 구현
    }
}
```

### Direct Chat 서비스 확장
```java
// DirectChatService.java에 새 모델 추가
Map<String, String> newModel = new HashMap<>();
newModel.put("name", "new-model-name");
newModel.put("size", "model-size");
models.add(newModel);
```

## 📊 모니터링

### Actuator 엔드포인트
- Health Check: `http://localhost:8080/actuator/health`
- Metrics: `http://localhost:8080/actuator/metrics`

### Swagger API 문서
- `http://localhost:8080/swagger-ui.html`

## 🐛 트러블슈팅

### 1. 포트 충돌 (8080)
```bash
# 사용 중인 프로세스 종료
lsof -ti:8080 | xargs kill -9

# 또는 다른 포트로 변경
java -jar -Dserver.port=8081 modules/rag-api/build/libs/rag-api-1.0.0-SNAPSHOT.jar
```

### 2. Elasticsearch 연결 실패
```bash
# Elasticsearch 상태 확인
curl -X GET "localhost:9200/_cluster/health?pretty"

# 인덱스 수동 생성
curl -X PUT "localhost:9200/rag-chunks"
```

### 3. MySQL 연결 실패
```bash
# MySQL 서비스 확인
mysql -u nerget-user -p nerget

# 권한 확인
SHOW GRANTS FOR 'nerget-user'@'localhost';
```

### 4. Ollama 모델 다운로드
```bash
# 원격 서버에서 모델 pull (필요한 경우)
ollama pull llama3.2-vision:11b
ollama pull nomic-embed-text
```

## 📂 프로젝트 구조 상세

```
modules/
├── common/
│   └── src/main/java/com/dsai/rag/common/
│       ├── dto/BaseResponse.java       # 통합 응답 포맷
│       └── exception/                  # 예외 처리
│
├── model/
│   └── src/main/java/com/dsai/rag/model/
│       ├── ChatMessage.java
│       ├── ChatRequest.java
│       ├── ChatResponse.java
│       ├── Document.java
│       └── QueryRequest.java
│
├── rag-core/
│   └── src/main/java/com/dsai/rag/core/
│       ├── service/
│       │   ├── ChatService.java        # 채팅 서비스
│       │   ├── DirectChatService.java  # Direct Ollama 서비스
│       │   └── RagOrchestrator.java    # RAG 오케스트레이터
│       └── graph/
│           └── ChatWorkflow.java       # 채팅 워크플로우
│
└── rag-api/
    ├── src/main/java/com/dsai/rag/api/
    │   └── controller/
    │       ├── ChatController.java
    │       ├── DirectChatController.java
    │       └── DocumentController.java
    └── src/main/resources/
        ├── application.yml
        └── static/
            ├── index.html
            ├── chat.js
            └── chat.css
```

## 🔄 향후 개선 사항

1. **인증 및 권한 관리** 추가
2. **대화 히스토리 영구 저장** (현재 메모리)
3. **파일 업로드 진행 상황** 표시
4. **다중 언어 지원**
5. **벡터 검색 성능 최적화**
6. **모델 자동 선택** 로직 구현
7. **웹소켓 기반 실시간 통신**
8. **대시보드 및 분석 기능**

## 📜 라이선스

[라이선스 정보 추가]

## 🤝 기여 방법

[기여 가이드라인 추가]

## 📞 문의

[연락처 정보 추가]

---

마지막 업데이트: 2025-01-19