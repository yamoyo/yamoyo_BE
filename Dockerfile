# ========================================
# 1. Build Stage
# ========================================
FROM eclipse-temurin:21-jdk AS builder

WORKDIR /app

# Gradle wrapper 및 빌드 파일 복사
COPY gradle gradle
COPY gradlew build.gradle settings.gradle ./

# Gradle 실행 권한 부여 및 의존성 캐시
RUN chmod +x gradlew && ./gradlew dependencies --no-daemon

# 소스 코드 복사 및 빌드 (테스트 스킵)
COPY src src
RUN ./gradlew build -x test --no-daemon

# ========================================
# 2. Runtime Stage
# ========================================
FROM eclipse-temurin:21-jre

WORKDIR /app

# JAR 파일 복사
COPY --from=builder /app/build/libs/app.jar app.jar

# 8080 포트 노출
EXPOSE 8080

# Health check (wget 사용 - jre 이미지에 포함, spring-actuator 의존성 필요)
HEALTHCHECK --interval=30s --timeout=3s --start-period=60s --retries=3 \
  CMD wget --spider -q http://localhost:8080/api/actuator/health || exit 1

# 애플리케이션 실행
ENTRYPOINT ["java", "-jar", "app.jar"]
