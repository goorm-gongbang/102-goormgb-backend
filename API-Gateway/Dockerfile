FROM gradle:jdk21-alpine AS builder
WORKDIR /app

# 1. 프로젝트 설정 파일 복사 (Gradle 래퍼 불필요)
COPY build.gradle .
COPY settings.gradle .

# 2. 모든 모듈의 build.gradle 복사 (Gradle 멀티 모듈 구조 유지 필수)
COPY API-Gateway/build.gradle API-Gateway/
COPY Auth-Guard/build.gradle Auth-Guard/
COPY common-core/build.gradle common-core/
COPY Order-Core/build.gradle Order-Core/
COPY Queue/build.gradle Queue/
COPY Seat/build.gradle Seat/
COPY Recommendation/build.gradle Recommendation/

# 3. 필요한 소스 코드만 복사 (현재 서비스만 — common-core 의존 없음)
COPY API-Gateway/src API-Gateway/src

# 4. 빌드 수행 (테스트 제외)
RUN gradle :API-Gateway:bootJar -x test --no-daemon

FROM eclipse-temurin:21-jre-alpine
WORKDIR /app
COPY --from=builder /app/API-Gateway/build/libs/*.jar app.jar
ENTRYPOINT ["java", "-jar", "app.jar"]
