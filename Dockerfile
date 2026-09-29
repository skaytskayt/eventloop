# Сборка
FROM maven:3.9-eclipse-temurin-21 AS build
WORKDIR /build

# Слой зависимостей кешируется отдельно от исходников
COPY pom.xml .
RUN mvn -B -q dependency:go-offline

COPY src ./src
RUN mvn -B -q clean package -DskipTests

# Запуск
FROM eclipse-temurin:21-jre
WORKDIR /app

# Русская локаль нужна для названий месяцев и дней недели в карточках
ENV LANG=ru_RU.UTF-8
ENV JAVA_TOOL_OPTIONS="-Dfile.encoding=UTF-8 -Duser.timezone=Europe/Moscow"

# Сертификаты Минцифры нужны для platform-api2.max.ru и в образе отсутствуют.
# Их везёт с собой SDK — если однажды перестанет, добавлять корневой сертификат
# в trust store JVM нужно именно здесь.

# curl нужен healthcheck'у в compose; в базовом образе его нет
RUN apt-get update \
 && apt-get install -y --no-install-recommends curl \
 && rm -rf /var/lib/apt/lists/*

COPY --from=build /build/target/*.jar app.jar

RUN useradd --system --create-home bot
USER bot

EXPOSE 8080
ENTRYPOINT ["java", "-jar", "/app/app.jar"]
