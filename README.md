# Order Payment Microservices

주문과 결제를 처리하는 마이크로서비스 기반 시스템입니다.

## 서비스 구성

### 1. Auth Service (8081)
- 사용자 인증 및 인가
- 회원 관리 (가입, 수정, 조회)
- JWT 토큰 관리
- 사용자 권한 관리
- 프로필 관리
- 엔드포인트: `/api/auth/**`, `/api/users/**`

### 2. Order Service (8082)
- 주문 생성 및 관리
- 주문 상태 관리
- 주문 이력 조회
- 주문 이벤트 발행
- 엔드포인트: `/api/orders/**`

### 3. Payment Service (8083)
- 결제 처리
- 결제 상태 관리
- 결제 이력 조회
- 결제 이벤트 발행
- 엔드포인트: `/api/payments/**`

### 4. Gateway Service (8000)
- API 라우팅
- 로드 밸런싱
- 서비스 디스커버리 연동

### 5. Eureka Server (8761)
- 서비스 디스커버리
- 서비스 등록 및 관리
- 상태 모니터링

### 6. Common Library
- 공통 DTO
- 이벤트 메시지
- 유틸리티 클래스
- 공통 예외 처리

## 시스템 구성도
```mermaid
graph TD
    Client[Client] --> Gateway[Gateway Service :8000]
    Gateway --> Auth[Auth Service :8081]
    Gateway --> Order[Order Service :8082]
    Gateway --> Payment[Payment Service :8083]
    Order --> Kafka[Kafka :9092]
    Payment --> Kafka
    Order --> Auth
    Payment --> Auth
    All[All Services] --> Eureka[Eureka Server :8761]
```

## 서비스 포트 구성

| 서비스 | 포트 | 엔드포인트 | 설명 |
|--------|------|------------|------|
| Gateway Service | 8000 | / | API Gateway |
| Eureka Server | 8761 | /eureka | 서비스 디스커버리 |
| Auth Service | 8081 | /api/auth/**, /api/users/** | 인증 및 사용자 관리 |
| Order Service | 8082 | /api/orders/** | 주문 관리 |
| Payment Service | 8083 | /api/payments/** | 결제 처리 |
| Kafka | 9092 | - | 메시지 브로커 |
| Zookeeper | 2181 | - | Kafka 클러스터 관리 |

## 기술 스택
- Java 11
- Spring Boot 2.7.x
- Spring Cloud
- Spring Security
- Spring Data JPA
- MySQL
- Kafka
- Docker
- Gradle

## 시작하기

1. 저장소 클론:
```bash
git clone https://github.com/yongchulShin/order-payment-ms.git
cd order-payment-ms
```

2. 도커 컴포즈로 실행:
```bash
docker-compose up -d
```

3. 서비스 접속:
- API Gateway: http://localhost:8000
- Eureka Server: http://localhost:8761
- Auth Service: http://localhost:8081 (인증 및 사용자 관리)
- Order Service: http://localhost:8082 (주문 관리)
- Payment Service: http://localhost:8083 (결제 처리)

## API 문서
각 서비스의 API 문서는 Swagger UI를 통해 확인할 수 있습니다:
- Auth Service: http://localhost:8081/swagger-ui.html
- Order Service: http://localhost:8082/swagger-ui.html
- Payment Service: http://localhost:8083/swagger-ui.html

## 기술 스택

- **프레임워크**: Spring Boot, Spring Cloud
- **보안**: Spring Security with JWT
- **데이터베이스**: MySQL
- **메시지 브로커**: Apache Kafka
- **서비스 디스커버리**: Netflix Eureka
- **API Gateway**: Spring Cloud Gateway
- **컨테이너화**: Docker
- **빌드 도구**: Gradle

## 주요 기능

### 인증 및 보안
- JWT 기반 인증
- 액세스 토큰 (5분 유효)
- 리프레시 토큰 (1개월 유효)
- 리프레시 토큰 교체(RTR) 메커니즘
- 역할 기반 접근 제어

### 주문 관리
- 주문 생성 및 추적
- 주문 상태 관리
- 주문 항목 처리
- 실시간 주문 업데이트

### 결제 처리
- 안전한 결제 처리
- 결제 상태 추적
- 결제 내역
- 트랜잭션 관리

### 이벤트 기반 아키텍처
- Kafka 기반 이벤트 통신
- 비동기 처리
- 중요 작업에 대한 이벤트 소싱

#### 이벤트 흐름
주문-결제 프로세스는 다음과 같은 이벤트 흐름을 따릅니다:

```mermaid
sequenceDiagram
    participant OS as Order Service
    participant K as Kafka
    participant PS as Payment Service
    
    OS->>OS: 주문 생성 (CREATED)
    OS->>K: OrderCreatedEvent 발행
    PS->>K: OrderCreatedEvent 구독
    PS->>PS: 결제 처리
    PS->>K: PaymentProcessedEvent 발행
    OS->>K: PaymentProcessedEvent 구독
    OS->>OS: 주문 상태 업데이트 (PAID/FAILED)
```

1. **주문 생성 이벤트**
   - Order Service에서 주문 생성 시 `order-created` 토픽으로 이벤트 발행
   - 이벤트 데이터: 주문ID, 사용자ID, 총액(BigDecimal)

2. **결제 처리 이벤트**
   - Payment Service에서 결제 처리 후 `payment-processed` 토픽으로 이벤트 발행
   - 이벤트 데이터: 주문ID, 결제ID, 금액(BigDecimal), 상태(SUCCESS/FAILED), 실패 사유

3. **주문 상태 흐름**
   - CREATED: 주문 생성 시
   - PENDING: 결제 대기 상태
   - PAID: 결제 완료 상태
   - COMPLETED: 주문 처리 완료 상태
   - FAILED: 결제 실패 상태
   - CANCELLED: 주문 취소 상태

4. **에러 처리**
   - 결제 실패 시 자동 실패 처리
   - 상세 실패 사유 기록
   - 실패 이벤트를 통한 상태 동기화

5. **데이터 정확성**
   - 모든 금액은 BigDecimal 사용
   - 주문 금액, 결제 금액, 상품 가격 등 정확한 계산 보장
   - 반올림 오류 방지

## 보안 고려사항

- 모든 민감한 데이터 암호화
- 토큰 안전한 저장 및 관리
- 역할 기반 접근 제어 구현
- 서비스 간 보안 통신
- API 엔드포인트 요청 제한

## 모니터링 및 로깅

- 중앙 집중식 로깅 시스템
- 성능 메트릭 수집
- 서비스 상태 모니터링
- 트랜잭션 추적

## 기여하기

1. 저장소 포크
2. 기능 브랜치 생성
3. 변경사항 커밋
4. 브랜치에 푸시
5. Pull Request 생성

