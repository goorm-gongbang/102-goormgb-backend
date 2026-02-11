FROM gradle:jdk21-alpine AS builder
WORKDIR /app

# 1. 프로젝트 설정 파일 복사 (Gradle 래퍼 불필요)
COPY build.gradle .
COPY settings.gradle .

# 2. 모든 모듈의 build.gradle 복사 (Gradle 멀티 모듈 구조 유지 필수)
COPY Auth-Guard/build.gradle Auth-Guard/
COPY common-core/build.gradle common-core/
COPY Order-Core/build.gradle Order-Core/
COPY Queue/build.gradle Queue/
COPY Seat/build.gradle Seat/
COPY Recommendation/build.gradle Recommendation/

# 3. 필요한 소스 코드만 복사 (공통 모듈 + 현재 서비스)
COPY common-core/src common-core/src
COPY Recommendation/src Recommendation/src

# 4. 빌드 수행 (테스트 제외)
RUN gradle :Recommendation:bootJar -x test --no-daemon

FROM eclipse-temurin:21-jre-alpine
WORKDIR /app
COPY --from=builder /app/Recommendation/build/libs/*.jar app.jar
ENTRYPOINT ["java", "-jar", "app.jar"]
