# ===========================================
# Stage 1: BUILD — собираем jar через Maven
# ===========================================
FROM maven:3.9-eclipse-temurin-21 AS build
WORKDIR /app

# Сначала pom.xml — слой зависимостей кэшируется отдельно от src
COPY pom.xml .

# Локальные артефакты org.apache.lucene.morphology:1.5 — в публичных
# репозиториях (jitpack/central) их больше нет. Ставим в .m2 контейнера
# ДО go-offline/package, иначе Maven не сможет их разрешить.
COPY lib ./lib
RUN mvn -B install:install-file -Dfile=lib/morphology-1.5.jar        -DpomFile=lib/morphology-1.5.pom \
 && mvn -B install:install-file -Dfile=lib/dictionary-reader-1.5.jar -DpomFile=lib/dictionary-reader-1.5.pom \
 && mvn -B install:install-file -Dfile=lib/morph-1.5.jar             -DpomFile=lib/morph-1.5.pom \
 && mvn -B install:install-file -Dfile=lib/russian-1.5.jar           -DpomFile=lib/russian-1.5.pom \
 && mvn -B install:install-file -Dfile=lib/english-1.5.jar           -DpomFile=lib/english-1.5.pom

# Скачиваем остальные зависимости (после установки локальных)
RUN mvn -B dependency:go-offline -DskipTests || true

# Потом исходники
COPY src ./src

# Сборка без тестов (тесты гоняем отдельно, в Блоке 2)
RUN mvn -B clean package -DskipTests

# ===========================================
# Stage 2: RUNTIME — тонкий JRE-образ
# ===========================================
FROM eclipse-temurin:21-jre
WORKDIR /app

# Директория для логов (application.yaml пишет в logs/debug.log)
RUN mkdir -p logs

# Копируем только jar из build-стадии
COPY --from=build /app/target/searchengine-1.0-SNAPSHOT.jar app.jar

EXPOSE 8080

# JVM уважает лимиты контейнера по памяти
ENV JAVA_OPTS="-XX:MaxRAMPercentage=75.0"

ENTRYPOINT ["sh", "-c", "java $JAVA_OPTS -jar app.jar"]