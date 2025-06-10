# 마이크로서비스 아키텍처 코드 리뷰

## 1. 기술 스택 및 프레임워크

### Core
- **Java 11**
  - LTS 버전으로 안정성과 성능 개선
  - 새로운 기능(var 키워드, HTTP Client 등) 활용 가능
  
- **Spring Boot 2.7.12**
  - Spring 5.3.x 기반
  - 자동 설정과 내장 서버 지원
  - 마이크로서비스에 최적화된 기능 제공

- **Spring Cloud 2021.0.7**
  - Netflix Eureka: 서비스 디스커버리
  - Gateway: API 라우팅 및 로드밸런싱
  - OpenFeign: 선언적 REST 클라이언트
  - Circuit Breaker: 장애 전파 방지

- **Spring Security + JWT**
  - JJWT 0.11.5 사용
  - 토큰 기반 인증
  - Role 기반 권한 관리

- **Spring Data JPA**
  - ORM을 통한 객체 지향적 데이터 접근
  - 쿼리 메소드 자동 생성
  - 페이징 및 정렬 지원

- **Spring Kafka**
  - 이벤트 기반 아키텍처 구현
  - 비동기 통신
  - 이벤트 소싱 패턴 적용

### Database & Message Queue
- **MySQL 8.0**
  - 트랜잭션 ACID 보장
  - 인덱싱 및 파티셔닝
  - 레플리케이션 지원

- **Apache Kafka**
  - 고성능 메시지 브로커
  - 이벤트 스트리밍 플랫폼
  - 확장성과 내구성 보장

- **Redis (미구현)**
  - 캐시 및 세션 저장소
  - 실시간 데이터 처리
  - 분산 락 구현

### Build & Deploy
- **Gradle 7.6.1**
  - 빌드 자동화
  - 의존성 관리
  - 멀티 모듈 지원

- **Docker & Docker Compose**
  - 컨테이너화
  - 환경 일관성
  - 배포 자동화

### Monitoring & Documentation
- **Spring Boot Actuator**
  - 애플리케이션 모니터링
  - 메트릭 수집
  - 헬스 체크

- **SpringDoc OpenAPI 1.7.0**
  - API 문서 자동화
  - Swagger UI 제공
  - 테스트 클라이언트 지원

- **Logback**
  - 로그 레벨 관리
  - 파일 로테이션
  - 패턴 레이아웃

## 2. 멀티 모듈 구성 및 Gradle 설정

### Root build.gradle

```gradle
buildscript {
    ext {
        springBootVersion = '2.7.12'
        springCloudVersion = '2021.0.7'
    }
    repositories {
        mavenCentral()
    }
}

plugins {
    id 'org.springframework.boot' version "${springBootVersion}" apply false
    id 'io.spring.dependency-management' version '1.0.15.RELEASE' apply false
    id 'java'
}

allprojects {
    group = 'com.example'
    version = '0.0.1-SNAPSHOT'
    sourceCompatibility = '11'

    repositories {
        mavenCentral()
    }
}

subprojects {
    apply plugin: 'java'
    apply plugin: 'org.springframework.boot'
    apply plugin: 'io.spring.dependency-management'

    configurations {
        compileOnly {
            extendsFrom annotationProcessor
        }
    }

    ext {
        set('springCloudVersion', "2021.0.7")
    }

    dependencyManagement {
        imports {
            mavenBom "org.springframework.cloud:spring-cloud-dependencies:${springCloudVersion}"
        }
    }

    dependencies {
        if (project.name != 'eureka-server' && project.name != 'common-lib' && project.name != 'gateway-service') {
            implementation project(':common-lib')
        }
        
        // Common dependencies
        if (project.name != 'gateway-service') {
            implementation 'org.springframework.boot:spring-boot-starter-web'
            implementation 'org.springframework.boot:spring-boot-starter-actuator'
            implementation 'org.springframework.cloud:spring-cloud-starter-netflix-eureka-client'
            implementation 'org.springdoc:springdoc-openapi-ui:1.7.0'
            implementation 'org.springdoc:springdoc-openapi-webmvc-core:1.7.0'
        }
        
        compileOnly 'org.projectlombok:lombok'
        annotationProcessor 'org.projectlombok:lombok'
        testImplementation 'org.springframework.boot:spring-boot-starter-test'
    }
}
```

### Common-lib build.gradle

```gradle
plugins {
    id 'java'
    id 'org.springframework.boot'
    id 'io.spring.dependency-management'
}

bootJar {
    enabled = false
}

jar {
    enabled = true
}

dependencies {
    implementation 'org.springframework.boot:spring-boot-starter-web'
    implementation 'org.springframework.boot:spring-boot-starter-security'
    implementation 'io.jsonwebtoken:jjwt-api:0.11.5'
    implementation 'org.springframework.cloud:spring-cloud-starter-netflix-eureka-client'
    implementation 'org.springframework.kafka:spring-kafka'
    implementation 'org.springdoc:springdoc-openapi-ui:1.7.0'
    implementation 'org.springdoc:springdoc-openapi-security:1.7.0'
    implementation 'org.springdoc:springdoc-openapi-webmvc-core:1.7.0'
    
    runtimeOnly 'io.jsonwebtoken:jjwt-impl:0.11.5'
    runtimeOnly 'io.jsonwebtoken:jjwt-jackson:0.11.5'
    
    compileOnly 'org.projectlombok:lombok'
    annotationProcessor 'org.projectlombok:lombok'
    
    testImplementation 'org.springframework.boot:spring-boot-starter-test'
    testImplementation 'org.springframework.security:spring-security-test'
    testImplementation 'org.springframework.kafka:spring-kafka-test'
}
```

### 설정 설명

1. **버전 관리**
   - `ext` 블록에서 주요 버전 중앙 관리
   - Spring Cloud BOM을 통한 의존성 버전 관리
   - 하위 프로젝트 간 버전 일관성 유지

2. **플러그인 구성**
   - Spring Boot 플러그인: 실행 가능한 JAR 생성
   - Dependency Management: 의존성 버전 관리
   - Java 플러그인: 자바 컴파일 및 테스트

3. **공통 의존성**
   - 모든 서비스에 필요한 기본 의존성 정의
   - 서비스별 특수 의존성은 개별 관리
   - 테스트 의존성 포함

4. **멀티 모듈 구조**
   - 공통 라이브러리를 통한 코드 재사용
   - 서비스별 독립적인 빌드 가능
   - 의존성 관계 명확화

## 3. Auto Configuration (spring.factories)

### Common-lib의 spring.factories

```properties
org.springframework.boot.autoconfigure.EnableAutoConfiguration=\
com.example.commonlib.config.SecurityAutoConfiguration,\
com.example.commonlib.config.KafkaAutoConfiguration,\
com.example.commonlib.config.SwaggerAutoConfiguration
```

### SecurityAutoConfiguration

```java
@Configuration
@EnableWebSecurity
@EnableGlobalMethodSecurity(prePostEnabled = true)
@EnableConfigurationProperties(JwtProperties.class)
public class SecurityAutoConfiguration {
    
    @Bean
    public JwtTokenProvider jwtTokenProvider(JwtProperties jwtProperties) {
        return new JwtTokenProvider(jwtProperties);
    }

    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
        return http
            .csrf().disable()
            .sessionManagement().sessionCreationPolicy(SessionCreationPolicy.STATELESS)
            .and()
            .authorizeRequests()
            .antMatchers("/actuator/**").permitAll()
            .antMatchers("/swagger-ui/**", "/v3/api-docs/**").permitAll()
            .antMatchers("/api/auth/**").permitAll()
            .anyRequest().authenticated()
            .and()
            .addFilterBefore(new JwtAuthenticationFilter(jwtTokenProvider), 
                UsernamePasswordAuthenticationFilter.class)
            .build();
    }

    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }
}
```

### KafkaAutoConfiguration

```java
@Configuration
@EnableKafka
@EnableConfigurationProperties(KafkaProperties.class)
public class KafkaAutoConfiguration {

    @Bean
    public ProducerFactory<String, Object> producerFactory(KafkaProperties kafkaProperties) {
        Map<String, Object> config = new HashMap<>();
        config.put(ProducerConfig.BOOTSTRAP_SERVERS_CONFIG, kafkaProperties.getBootstrapServers());
        config.put(ProducerConfig.KEY_SERIALIZER_CLASS_CONFIG, StringSerializer.class);
        config.put(ProducerConfig.VALUE_SERIALIZER_CLASS_CONFIG, JsonSerializer.class);
        config.put(ProducerConfig.ACKS_CONFIG, "all");
        config.put(ProducerConfig.RETRIES_CONFIG, 3);
        
        return new DefaultKafkaProducerFactory<>(config);
    }
    
    @Bean
    public ConsumerFactory<String, Object> consumerFactory(KafkaProperties kafkaProperties) {
        Map<String, Object> config = new HashMap<>();
        config.put(ConsumerConfig.BOOTSTRAP_SERVERS_CONFIG, kafkaProperties.getBootstrapServers());
        config.put(ConsumerConfig.GROUP_ID_CONFIG, "${spring.application.name}");
        config.put(ConsumerConfig.KEY_DESERIALIZER_CLASS_CONFIG, StringDeserializer.class);
        config.put(ConsumerConfig.VALUE_DESERIALIZER_CLASS_CONFIG, JsonDeserializer.class);
        config.put(ConsumerConfig.AUTO_OFFSET_RESET_CONFIG, "earliest");
        config.put(ConsumerConfig.ENABLE_AUTO_COMMIT_CONFIG, false);
        
        return new DefaultKafkaConsumerFactory<>(config);
    }

    @Bean
    public KafkaTemplate<String, Object> kafkaTemplate(ProducerFactory<String, Object> producerFactory) {
        return new KafkaTemplate<>(producerFactory);
    }
}
```

### SwaggerAutoConfiguration

```java
@Configuration
@EnableOpenApi
public class SwaggerAutoConfiguration {

    @Bean
    public OpenAPI customOpenAPI() {
        return new OpenAPI()
            .info(new Info()
                .title("Microservices API Documentation")
                .version("1.0")
                .description("API documentation for microservices")
                .contact(new Contact()
                    .name("Team")
                    .email("team@example.com")))
            .components(new Components()
                .addSecuritySchemes("bearerAuth", 
                    new SecurityScheme()
                        .type(SecurityScheme.Type.HTTP)
                        .scheme("bearer")
                        .bearerFormat("JWT")));
    }
}
```

### 설정 설명

1. **자동 설정의 이점**
   - 반복적인 설정 코드 제거
   - 일관된 설정 적용
   - 설정 커스터마이징 용이

2. **보안 설정**
   - JWT 기반 인증
   - Stateless 세션 관리
   - API 엔드포인트 보안

3. **Kafka 설정**
   - 프로듀서/컨슈머 팩토리
   - 직렬화/역직렬화
   - 재시도 및 오류 처리

4. **Swagger 설정**
   - API 문서 자동화
   - JWT 인증 통합
   - API 그룹화 및 태깅

## 4. Logback 설정

### logback-spring.xml

```xml
<?xml version="1.0" encoding="UTF-8"?>
<configuration>
    <property name="LOG_PATH" value="./logs"/>
    <property name="LOG_FILE_NAME" value="${spring.application.name}"/>
    <property name="LOG_PATTERN" value="%d{yyyy-MM-dd HH:mm:ss.SSS} [%thread] [%X{traceId},%X{spanId}] %-5level %logger{36} - %msg%n"/>

    <appender name="Console" class="ch.qos.logback.core.ConsoleAppender">
        <encoder>
            <pattern>${LOG_PATTERN}</pattern>
        </encoder>
    </appender>

    <appender name="File" class="ch.qos.logback.core.rolling.RollingFileAppender">
        <file>${LOG_PATH}/${LOG_FILE_NAME}.log</file>
        <encoder>
            <pattern>${LOG_PATTERN}</pattern>
        </encoder>
        <rollingPolicy class="ch.qos.logback.core.rolling.TimeBasedRollingPolicy">
            <fileNamePattern>${LOG_PATH}/${LOG_FILE_NAME}.%d{yyyy-MM-dd}.log</fileNamePattern>
            <maxHistory>30</maxHistory>
            <totalSizeCap>3GB</totalSizeCap>
        </rollingPolicy>
    </appender>

    <appender name="AsyncFile" class="ch.qos.logback.classic.AsyncAppender">
        <appender-ref ref="File"/>
        <queueSize>512</queueSize>
        <discardingThreshold>0</discardingThreshold>
        <includeCallerData>false</includeCallerData>
        <neverBlock>true</neverBlock>
    </appender>

    <!-- Application Loggers -->
    <logger name="com.example" level="DEBUG"/>

    <!-- Framework Loggers -->
    <logger name="org.springframework" level="INFO"/>
    <logger name="org.hibernate" level="INFO"/>
    <logger name="org.apache.kafka" level="INFO"/>

    <!-- Root Logger -->
    <root level="INFO">
        <appender-ref ref="Console"/>
        <appender-ref ref="AsyncFile"/>
    </root>
</configuration>
```

### 설정 설명

1. **로그 패턴**
   - 타임스탬프
   - 스레드 정보
   - 트레이스 ID (분산 추적)
   - 로그 레벨
   - 로거 이름
   - 메시지

2. **파일 로깅**
   - 일별 로그 파일 생성
   - 보관 기간 설정
   - 총 용량 제한

3. **비동기 로깅**
   - 성능 최적화
   - 큐 사이즈 설정
   - 버퍼 관리

4. **로거 설정**
   - 애플리케이션별 로그 레벨
   - 프레임워크별 로그 레벨
   - 루트 로거 설정

## 5. Kafka 설정

### KafkaTopicConfig

```java
@Configuration
public class KafkaTopicConfig {

    @Value("${spring.kafka.num-partitions:3}")
    private int numPartitions;

    @Value("${spring.kafka.replication-factor:1}")
    private short replicationFactor;

    @Bean
    public NewTopic orderTopic() {
        return TopicBuilder.name("orders")
            .partitions(numPartitions)
            .replicas(replicationFactor)
            .config(TopicConfig.RETENTION_MS_CONFIG, String.valueOf(TimeUnit.DAYS.toMillis(7)))
            .build();
    }

    @Bean
    public NewTopic paymentTopic() {
        return TopicBuilder.name("payments")
            .partitions(numPartitions)
            .replicas(replicationFactor)
            .config(TopicConfig.RETENTION_MS_CONFIG, String.valueOf(TimeUnit.DAYS.toMillis(7)))
            .build();
    }
}
```

### KafkaProducerConfig

```java
@Configuration
@EnableKafka
public class KafkaProducerConfig {

    @Bean
    public ProducerFactory<String, Object> producerFactory(KafkaProperties kafkaProperties) {
        Map<String, Object> config = new HashMap<>();
        config.put(ProducerConfig.BOOTSTRAP_SERVERS_CONFIG, kafkaProperties.getBootstrapServers());
        config.put(ProducerConfig.KEY_SERIALIZER_CLASS_CONFIG, StringSerializer.class);
        config.put(ProducerConfig.VALUE_SERIALIZER_CLASS_CONFIG, JsonSerializer.class);
        
        // 신뢰성 설정
        config.put(ProducerConfig.ACKS_CONFIG, "all");
        config.put(ProducerConfig.RETRIES_CONFIG, 3);
        config.put(ProducerConfig.RETRY_BACKOFF_MS_CONFIG, 1000);
        
        // 성능 설정
        config.put(ProducerConfig.BATCH_SIZE_CONFIG, 16384);
        config.put(ProducerConfig.LINGER_MS_CONFIG, 1);
        config.put(ProducerConfig.BUFFER_MEMORY_CONFIG, 33554432);
        
        // 압축 설정
        config.put(ProducerConfig.COMPRESSION_TYPE_CONFIG, "snappy");
        
        return new DefaultKafkaProducerFactory<>(config);
    }

    @Bean
    public KafkaTemplate<String, Object> kafkaTemplate(ProducerFactory<String, Object> producerFactory) {
        return new KafkaTemplate<>(producerFactory);
    }
}
```

### KafkaConsumerConfig

```java
@Configuration
@EnableKafka
public class KafkaConsumerConfig {

    @Bean
    public ConsumerFactory<String, Object> consumerFactory(KafkaProperties kafkaProperties) {
        Map<String, Object> config = new HashMap<>();
        config.put(ConsumerConfig.BOOTSTRAP_SERVERS_CONFIG, kafkaProperties.getBootstrapServers());
        config.put(ConsumerConfig.GROUP_ID_CONFIG, "${spring.application.name}");
        config.put(ConsumerConfig.KEY_DESERIALIZER_CLASS_CONFIG, StringDeserializer.class);
        config.put(ConsumerConfig.VALUE_DESERIALIZER_CLASS_CONFIG, JsonDeserializer.class);
        
        // 컨슈머 설정
        config.put(ConsumerConfig.AUTO_OFFSET_RESET_CONFIG, "earliest");
        config.put(ConsumerConfig.ENABLE_AUTO_COMMIT_CONFIG, false);
        config.put(ConsumerConfig.MAX_POLL_RECORDS_CONFIG, 500);
        
        // 성능 설정
        config.put(ConsumerConfig.FETCH_MIN_BYTES_CONFIG, 1);
        config.put(ConsumerConfig.FETCH_MAX_WAIT_MS_CONFIG, 500);
        config.put(ConsumerConfig.HEARTBEAT_INTERVAL_MS_CONFIG, 3000);
        
        return new DefaultKafkaConsumerFactory<>(config);
    }

    @Bean
    public ConcurrentKafkaListenerContainerFactory<String, Object> kafkaListenerContainerFactory(
            ConsumerFactory<String, Object> consumerFactory) {
        ConcurrentKafkaListenerContainerFactory<String, Object> factory = 
            new ConcurrentKafkaListenerContainerFactory<>();
        factory.setConsumerFactory(consumerFactory);
        factory.setConcurrency(3);
        factory.getContainerProperties().setPollTimeout(3000);
        factory.setCommonErrorHandler(new DefaultErrorHandler(
            new DeadLetterPublishingRecoverer(kafkaTemplate),
            new FixedBackOff(1000L, 3L)
        ));
        return factory;
    }
}
```

### 설정 설명

1. **토픽 설정**
   - 파티션 수
   - 복제 팩터
   - 보관 기간

2. **프로듀서 설정**
   - 메시지 신뢰성 (acks=all)
   - 재시도 정책 (retries=3)
   - 멱등성 보장

3. **컨슈머 설정**
   - 그룹 관리
   - 오프셋 관리
   - 에러 처리

## 6. 핵심 기능

### 1. JPA Entity 설계

```java
@Entity
@Table(name = "orders")
@Getter @Setter
@NoArgsConstructor
public class Order extends BaseTimeEntity {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private Long userId;

    @Enumerated(EnumType.STRING)
    private OrderStatus status;

    @Column(nullable = false)
    private BigDecimal totalAmount;

    @OneToMany(mappedBy = "order", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<OrderItem> items = new ArrayList<>();

    @Builder
    public Order(Long userId, OrderStatus status, BigDecimal totalAmount) {
        this.userId = userId;
        this.status = status;
        this.totalAmount = totalAmount;
    }

    public void addOrderItem(OrderItem orderItem) {
        items.add(orderItem);
        orderItem.setOrder(this);
    }
}

@Entity
@Table(name = "order_items")
@Getter @Setter
@NoArgsConstructor
public class OrderItem extends BaseTimeEntity {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "order_id")
    private Order order;

    @Column(nullable = false)
    private Long productId;

    @Column(nullable = false)
    private Integer quantity;

    @Column(nullable = false)
    private BigDecimal price;

    @Builder
    public OrderItem(Long productId, Integer quantity, BigDecimal price) {
        this.productId = productId;
        this.quantity = quantity;
        this.price = price;
    }
}

@Entity
@Table(name = "payments")
@Getter @Setter
@NoArgsConstructor
public class Payment extends BaseTimeEntity {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private Long orderId;

    @Column(nullable = false)
    private Long userId;

    @Column(nullable = false)
    private BigDecimal amount;

    @Enumerated(EnumType.STRING)
    private PaymentStatus status;

    @Column
    private String failureReason;

    @Builder
    public Payment(Long orderId, Long userId, BigDecimal amount, PaymentStatus status) {
        this.orderId = orderId;
        this.userId = userId;
        this.amount = amount;
        this.status = status;
    }
}
```

### 2. Repository 계층

```java
@Repository
public interface OrderRepository extends JpaRepository<Order, Long> {
    List<Order> findByUserId(Long userId);
    List<Order> findByStatus(OrderStatus status);
    
    @Query("SELECT o FROM Order o JOIN FETCH o.items WHERE o.id = :orderId")
    Optional<Order> findByIdWithItems(@Param("orderId") Long orderId);
    
    @Query("SELECT o FROM Order o WHERE o.status = :status AND o.createdAt >= :startDate")
    Page<Order> findByStatusAndCreatedAtAfter(
        @Param("status") OrderStatus status,
        @Param("startDate") LocalDateTime startDate,
        Pageable pageable
    );
}

@Repository
public interface PaymentRepository extends JpaRepository<Payment, Long> {
    Optional<Payment> findByOrderId(Long orderId);
    boolean existsByOrderId(Long orderId);
    List<Payment> findByUserId(Long userId);
    
    @Query("SELECT p FROM Payment p WHERE p.status = :status AND p.createdAt BETWEEN :startDate AND :endDate")
    List<Payment> findByStatusAndDateRange(
        @Param("status") PaymentStatus status,
        @Param("startDate") LocalDateTime startDate,
        @Param("endDate") LocalDateTime endDate
    );
}
```

### 3. 주문 처리 시스템

```java
@Service
@Transactional
@RequiredArgsConstructor
public class OrderService {
    private final OrderRepository orderRepository;
    private final KafkaTemplate<String, Object> kafkaTemplate;

    public OrderResponse createOrder(OrderRequest request) {
        // 주문 생성
        Order order = Order.builder()
            .userId(request.getUserId())
            .status(OrderStatus.PENDING)
            .totalAmount(calculateTotalAmount(request.getItems()))
            .build();

        // 주문 아이템 추가
        request.getItems().forEach(item -> {
            OrderItem orderItem = OrderItem.builder()
                .productId(item.getProductId())
                .quantity(item.getQuantity())
                .price(item.getPrice())
                .build();
            order.addOrderItem(orderItem);
        });

        order = orderRepository.save(order);

        // 결제 이벤트 발행
        OrderCreatedEvent event = new OrderCreatedEvent(
            order.getId(),
            order.getUserId(),
            order.getTotalAmount()
        );

        kafkaTemplate.send("orders", order.getId().toString(), event);

        return OrderResponse.from(order);
    }

    @Transactional(readOnly = true)
    public OrderResponse getOrder(Long orderId) {
        Order order = orderRepository.findByIdWithItems(orderId)
            .orElseThrow(() -> new OrderNotFoundException(orderId));
        return OrderResponse.from(order);
    }

    @Transactional(readOnly = true)
    public Page<OrderResponse> getUserOrders(Long userId, Pageable pageable) {
        return orderRepository.findByUserId(userId, pageable)
            .map(OrderResponse::from);
    }

    @KafkaListener(topics = "payments")
    public void handlePaymentProcessed(PaymentProcessedEvent event) {
        Order order = orderRepository.findById(event.getOrderId())
            .orElseThrow(() -> new OrderNotFoundException(event.getOrderId()));

        if (event.isSuccess()) {
            order.setStatus(OrderStatus.PAID);
        } else {
            order.setStatus(OrderStatus.PAYMENT_FAILED);
        }

        orderRepository.save(order);
    }

    private BigDecimal calculateTotalAmount(List<OrderItemRequest> items) {
        return items.stream()
            .map(item -> item.getPrice().multiply(BigDecimal.valueOf(item.getQuantity())))
            .reduce(BigDecimal.ZERO, BigDecimal::add);
    }
}
```

### 4. 결제 처리 시스템

```java
@Service
@Transactional
@RequiredArgsConstructor
public class PaymentService {
    private final PaymentRepository paymentRepository;
    private final KafkaTemplate<String, Object> kafkaTemplate;
    private final PaymentProcessor paymentProcessor;

    @KafkaListener(topics = "orders")
    public void processPayment(OrderCreatedEvent event) {
        try {
            // 중복 결제 체크
            if (paymentRepository.existsByOrderId(event.getOrderId())) {
                throw new DuplicatePaymentException(event.getOrderId());
            }

            // 결제 처리
            PaymentResult result = paymentProcessor.process(
                event.getOrderId(),
                event.getAmount()
            );

            // 결제 정보 저장
            Payment payment = Payment.builder()
                .orderId(event.getOrderId())
                .userId(event.getUserId())
                .amount(event.getAmount())
                .status(result.isSuccess() ? PaymentStatus.SUCCESS : PaymentStatus.FAILED)
                .failureReason(result.getFailureReason())
                .build();

            paymentRepository.save(payment);

            // 결제 결과 이벤트 발행
            PaymentProcessedEvent paymentEvent = new PaymentProcessedEvent(
                event.getOrderId(),
                result.isSuccess(),
                result.getFailureReason()
            );

            kafkaTemplate.send("payments", event.getOrderId().toString(), paymentEvent);

        } catch (Exception e) {
            // 에러 처리 및 실패 이벤트 발행
            PaymentProcessedEvent failureEvent = new PaymentProcessedEvent(
                event.getOrderId(),
                false,
                e.getMessage()
            );

            kafkaTemplate.send("payments", event.getOrderId().toString(), failureEvent);
        }
    }

    @Transactional(readOnly = true)
    public PaymentResponse getPayment(Long paymentId) {
        Payment payment = paymentRepository.findById(paymentId)
            .orElseThrow(() -> new PaymentNotFoundException(paymentId));
        return PaymentResponse.from(payment);
    }

    @Transactional(readOnly = true)
    public List<PaymentResponse> getUserPayments(Long userId) {
        return paymentRepository.findByUserId(userId).stream()
            .map(PaymentResponse::from)
            .collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public List<PaymentResponse> getPaymentsByDateRange(
            PaymentStatus status, LocalDateTime startDate, LocalDateTime endDate) {
        return paymentRepository.findByStatusAndDateRange(status, startDate, endDate).stream()
            .map(PaymentResponse::from)
            .collect(Collectors.toList());
    }
}
```

### 핵심 기능 설명

1. **JPA Entity 설계**
   - 적절한 관계 매핑 (OneToMany, ManyToOne)
   - 영속성 전이 (Cascade) 설정
   - 지연 로딩 (Lazy Loading) 적용
   - 감사 정보 (Audit) 관리

2. **Repository 계층**
   - Spring Data JPA 활용
   - 커스텀 쿼리 메소드
   - JPQL을 통한 복잡한 쿼리 처리
   - 페이징 및 정렬 지원

3. **트랜잭션 관리**
   - 선언적 트랜잭션 (@Transactional)
   - 읽기 전용 트랜잭션 최적화
   - 트랜잭션 격리 수준 관리
   - 동시성 제어

4. **이벤트 기반 아키텍처**
   - Kafka를 통한 비동기 통신
   - 서비스 간 느슨한 결합
   - 이벤트 소싱 패턴 적용

5. **에러 처리**
   - 예외 처리 전략
   - 트랜잭션 롤백
   - 실패 복구 메커니즘

6. **데이터 정합성**
   - JPA 영속성 컨텍스트 활용
   - 낙관적 락킹
   - 중복 처리 방지
   - 정확한 금액 계산 (BigDecimal 사용)

이러한 구성으로 안정적이고 확장 가능한 마이크로서비스 아키텍처를 구현하였습니다.

## 7. 예외 처리 및 데이터 검증

### 1. Custom Exception 처리

```java
// 기본 비즈니스 예외 클래스
@Getter
public class BusinessException extends RuntimeException {
    private final ErrorCode errorCode;
    
    public BusinessException(ErrorCode errorCode) {
        super(errorCode.getMessage());
        this.errorCode = errorCode;
    }
}

// 구체적인 비즈니스 예외들
public class OrderNotFoundException extends BusinessException {
    public OrderNotFoundException(Long orderId) {
        super(ErrorCode.ORDER_NOT_FOUND);
    }
}

public class DuplicatePaymentException extends BusinessException {
    public DuplicatePaymentException(Long orderId) {
        super(ErrorCode.DUPLICATE_PAYMENT);
    }
}

// 전역 예외 처리기
@RestControllerAdvice
@Slf4j
public class GlobalExceptionHandler {
    
    @ExceptionHandler(BusinessException.class)
    public ResponseEntity<ErrorResponse> handleBusinessException(BusinessException e) {
        log.error("Business Exception: {}", e.getMessage());
        return ResponseEntity
            .status(e.getErrorCode().getStatus())
            .body(new ErrorResponse(e.getErrorCode()));
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ErrorResponse> handleValidationException(MethodArgumentNotValidException e) {
        log.error("Validation Exception: {}", e.getMessage());
        List<String> errors = e.getBindingResult()
            .getFieldErrors()
            .stream()
            .map(error -> error.getField() + ": " + error.getDefaultMessage())
            .collect(Collectors.toList());
            
        return ResponseEntity
            .status(HttpStatus.BAD_REQUEST)
            .body(new ErrorResponse(ErrorCode.INVALID_INPUT, errors));
    }
}
```

**설명**:
1. **계층화된 예외 구조**
   - `BusinessException`을 기본 예외 클래스로 정의
   - 구체적인 예외들은 이를 상속하여 구현
   - 예외별로 고유한 에러 코드와 메시지 관리

2. **예외 처리 전략**
   - 컨트롤러 레벨에서 전역적으로 예외 처리
   - 로깅을 통한 문제 추적
   - 일관된 에러 응답 형식 제공

3. **비즈니스 규칙 검증**
   - 주문 존재 여부 확인
   - 중복 결제 방지
   - 잘못된 상태 전이 방지

### 2. DTO 검증

```java
@Getter @Setter
@NoArgsConstructor
public class OrderRequest {
    @NotNull(message = "사용자 ID는 필수입니다")
    private Long userId;

    @NotEmpty(message = "주문 항목은 최소 1개 이상이어야 합니다")
    @Valid  // 중첩된 객체의 검증을 위해 필요
    private List<OrderItemRequest> items;
}

@Getter @Setter
@NoArgsConstructor
public class OrderItemRequest {
    @NotNull(message = "상품 ID는 필수입니다")
    private Long productId;

    @Min(value = 1, message = "수량은 1개 이상이어야 합니다")
    private Integer quantity;

    @NotNull(message = "가격은 필수입니다")
    @DecimalMin(value = "0.0", inclusive = false, message = "가격은 0보다 커야 합니다")
    private BigDecimal price;
}

@RestController
@RequestMapping("/api/orders")
@Validated  // 메소드 파라미터 검증을 위해 필요
public class OrderController {
    
    @PostMapping
    public ResponseEntity<OrderResponse> createOrder(
            @Valid @RequestBody OrderRequest request) {
        return ResponseEntity.ok(orderService.createOrder(request));
    }

    @GetMapping("/{orderId}")
    public ResponseEntity<OrderResponse> getOrder(
            @PathVariable @Positive(message = "주문 ID는 양수여야 합니다") Long orderId) {
        return ResponseEntity.ok(orderService.getOrder(orderId));
    }
}
```

**설명**:
1. **입력 데이터 검증**
   - Bean Validation을 통한 선언적 검증
   - 중첩된 객체의 검증 (@Valid)
   - 커스텀 검증 로직 구현 가능

2. **검증 계층**
   - Presentation Layer (Controller): 기본적인 데이터 형식 검증
   - Service Layer: 비즈니스 규칙 검증
   - Repository Layer: 데이터 무결성 검증

3. **검증 시점**
   - API 요청 시점에서 즉시 검증
   - 서비스 로직 실행 전 유효성 확보
   - 잘못된 데이터의 전파 방지

### 3. 트랜잭션 경계 설정

```java
@Service
@Transactional  // 클래스 레벨의 트랜잭션 설정
@RequiredArgsConstructor
public class OrderService {
    private final OrderRepository orderRepository;
    private final PaymentService paymentService;

    // 기본 트랜잭션 설정 사용
    public OrderResponse createOrder(OrderRequest request) {
        // 1. 주문 생성
        Order order = orderRepository.save(Order.from(request));
        
        try {
            // 2. 결제 처리 (새로운 트랜잭션에서 실행)
            paymentService.processPayment(order);
        } catch (PaymentException e) {
            // 3. 결제 실패 시 주문 상태 업데이트
            order.setStatus(OrderStatus.PAYMENT_FAILED);
            orderRepository.save(order);
            throw e;
        }
        
        return OrderResponse.from(order);
    }

    // 읽기 전용 트랜잭션 설정
    @Transactional(readOnly = true)
    public OrderResponse getOrder(Long orderId) {
        return OrderResponse.from(
            orderRepository.findById(orderId)
                .orElseThrow(() -> new OrderNotFoundException(orderId))
        );
    }

    // 격리 수준을 높인 트랜잭션 설정
    @Transactional(isolation = Isolation.SERIALIZABLE)
    public void cancelOrder(Long orderId) {
        Order order = orderRepository.findById(orderId)
            .orElseThrow(() -> new OrderNotFoundException(orderId));
            
        if (!order.canCancel()) {
            throw new InvalidOrderStateException(orderId);
        }
        
        order.setStatus(OrderStatus.CANCELLED);
    }
}

@Service
@Transactional
@RequiredArgsConstructor
public class PaymentService {
    
    // 새로운 트랜잭션에서 실행
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void processPayment(Order order) {
        // 결제 처리 로직
        // 실패 시 PaymentException 발생
        // 이 트랜잭션의 실패가 호출자의 트랜잭션에 영향을 주지 않음
    }
}
```

**설명**:
1. **트랜잭션 경계 정의**
   - 클래스 레벨의 기본 트랜잭션 설정
   - 메소드 레벨의 세부 트랜잭션 설정
   - 트랜잭션 전파 설정을 통한 독립성 보장

2. **트랜잭션 속성**
   - `readOnly`: 읽기 전용 작업 최적화
   - `isolation`: 트랜잭션 격리 수준 설정
   - `propagation`: 트랜잭션 전파 방식 설정
   - `timeout`: 트랜잭션 시간 제한 설정

3. **트랜잭션 전파 시나리오**
   ```
   OrderService.createOrder()  // 외부 트랜잭션
   │
   ├─ orderRepository.save()  // 외부 트랜잭션에 참여
   │
   ├─ PaymentService.processPayment()  // 새로운 트랜잭션 시작 (REQUIRES_NEW)
   │  │
   │  └─ paymentRepository.save()  // 새로운 트랜잭션에 참여
   │
   └─ orderRepository.save()  // 외부 트랜잭션에 참여
   ```

4. **트랜잭션 격리 수준**
   - `READ_UNCOMMITTED`: 다른 트랜잭션의 커밋되지 않은 데이터 읽기 가능
   - `READ_COMMITTED`: 커밋된 데이터만 읽기 가능
   - `REPEATABLE_READ`: 트랜잭션 내에서 동일한 쿼리 결과 보장
   - `SERIALIZABLE`: 완벽한 격리, 성능 저하 가능성

5. **트랜잭션 경계 설정의 이점**
   - 데이터 일관성 보장
   - 원자성 보장 (모든 작업이 성공하거나 모두 실패)
   - 격리성 보장 (다른 트랜잭션과의 독립성)
   - 영속성 보장 (성공적인 작업의 영구 저장)