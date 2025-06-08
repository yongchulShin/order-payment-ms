# 주문-결제 마이크로서비스 시스템

사용자 인증 및 권한 부여가 포함된 견고한 마이크로서비스 기반 주문-결제 시스템입니다.

## 시스템 구조

시스템은 다음과 같은 마이크로서비스로 구성되어 있습니다:

- **Eureka Server**: 서비스 디스커버리 및 등록
- **API Gateway**: 모든 클라이언트 요청에 대한 단일 진입점
- **Auth Service**: 사용자 인증 및 권한 부여 처리
- **User Service**: 사용자 프로필 및 정보 관리
- **Order Service**: 주문 생성 및 관리
- **Payment Service**: 결제 처리 및 결제 내역 관리

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

## 시작하기

### 사전 요구사항
- JDK 11
- Docker 및 Docker Compose
- MySQL
- Apache Kafka

### 설치 방법

1. 저장소 클론:
```bash
git clone https://github.com/yourusername/order-payment-ms.git
cd order-payment-ms
```

2. 인프라 서비스 시작:
```bash
docker-compose up -d
```

3. 서비스 빌드 및 실행:
```bash
./gradlew build
```

4. 서비스 접속 주소:
- Eureka Server: http://localhost:8761
- API Gateway: http://localhost:8080
- Auth Service: http://localhost:8081
- User Service: http://localhost:8082
- Order Service: http://localhost:8083
- Payment Service: http://localhost:8084

## API 문서

### 인증 엔드포인트
- POST /api/auth/signup - 회원가입
- POST /api/auth/login - 로그인
- POST /api/auth/refresh - 액세스 토큰 갱신
- POST /api/auth/logout - 로그아웃

### 사용자 엔드포인트
- GET /api/users - 사용자 목록 조회
- GET /api/users/{id} - 사용자 상세 조회
- PUT /api/users/{id} - 사용자 정보 수정
- DELETE /api/users/{id} - 사용자 삭제

### 주문 엔드포인트
- POST /api/orders - 주문 생성
- GET /api/orders - 주문 목록 조회
- GET /api/orders/{id} - 주문 상세 조회
- PUT /api/orders/{id} - 주문 수정
- DELETE /api/orders/{id} - 주문 취소

### 결제 엔드포인트
- POST /api/payments - 결제 처리
- GET /api/payments - 결제 목록 조회
- GET /api/payments/{id} - 결제 상세 조회
- GET /api/payments/history - 결제 내역 조회

## 설정

각 서비스는 다음과 같은 설정을 포함하는 application.yml 파일을 가집니다:
- 데이터베이스 연결
- Kafka 설정
- 보안 매개변수
- 서비스 디스커버리
- 로깅 레벨

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

## 라이선스

이 프로젝트는 MIT 라이선스를 따릅니다 - 자세한 내용은 LICENSE 파일을 참조하세요.
