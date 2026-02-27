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

# 3. 의존성 다운로드 (소스 코드 복사 전)
# 소스 코드가 없으므로 빌드는 실패하지만, 의존성은 다운로드되어 캐시됩니다.
RUN gradle :API-Gateway:classes -x test --no-daemon || true

# 4. 필요한 소스 코드만 복사 (현재 서비스만 — common-core 의존 없음)
COPY API-Gateway/src API-Gateway/src

# 5. 실제 빌드 수행
RUN gradle :API-Gateway:bootJar -x test --no-daemon

FROM eclipse-temurin:21-jre-alpine
WORKDIR /app
COPY --from=builder /app/API-Gateway/build/libs/*.jar app.jar
ENTRYPOINT ["java", "-jar", "app.jar"]
