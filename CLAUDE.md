<!-- GSD:project-start source:PROJECT.md -->

## Project

**EventLoop — «Тиндер по местам» для Пушкинской карты**

Бот в мессенджере MAX, который подбирает мероприятия по Пушкинской карте в формате
свайпов. Пользователь вводит баланс карты, район и максимальное расстояние, отмечает
интересы (театр, кино, музеи, концерты, экскурсии) — и листает карточки мероприятий
с афишей, датой, ценой и расстоянием. Под каждой карточкой видно, сколько останется
на карте, если пойти именно сюда. Лайк даёт ссылку на мероприятие и покупку билета,
дизлайк убирает карточку, «в желаемое» откладывает на потом.

Аудитория: держатели Пушкинской карты (14–22 года) в Москве, которые не знают, на что
потратить баланс, и не хотят листать каталог из тысяч позиций.

**Core Value:** Человек за несколько свайпов находит мероприятие, на которое реально хватает баланса
и до которого он готов доехать, — и получает прямую ссылку на покупку билета.

### Constraints

- **Timeline**: хакатон / жёсткий дедлайн — нужен работающий демонстрируемый бот, а не идеальная архитектура. Режем углы там, где это не ломает демо.
- **Tech stack**: Spring Boot 4.1.1 + Java 21 + Maven — уже зафиксировано в `pom.xml`, менять не будем.
- **Deployment**: docker compose (приложение + БД) — должно подниматься одной командой на любой машине.
- **Dependencies**: MAX Bot API — единственный канал взаимодействия; возможности бота ограничены тем, что этот API умеет.
- **Data**: нет официального источника мероприятий Пушкинской карты — датасет наполняется вручную, объём ограничен.
- **Security**: токен бота и креды БД только через переменные окружения; в git не попадают.

<!-- GSD:project-end -->

<!-- GSD:stack-start source:research/STACK.md -->

## Technology Stack

## TL;DR — что решено

| Вопрос | Ответ | Уверенность |
|---|---|---|
| Java SDK для MAX Bot API | **Есть.** `ru.etsft.max:max-bot-api-spring-boot:0.5.0` — community, Apache-2.0, собран против Spring Boot **4.1.1** и Jackson **3.1.5** | HIGH |
| Транспорт | **Long polling.** Webhook требует публичный HTTPS — на хакатоне это лишняя возня | HIGH |
| Механика свайпа «одно сообщение, редактируемое на месте» | **Реализуема.** `POST /answers` с телом `message` заменяет исходное сообщение — один вызов на свайп | HIGH |
| Картинка по URL без загрузки файла | **Да**, `image.payload.url` | HIGH |
| БД | **PostgreSQL 17** в docker compose | HIGH |
| ORM | **Spring Data JPA** (не JDBC) | MEDIUM |
| Миграции | **Flyway 12.4.0** (managed версия SB 4.1.1) | HIGH |
| Гео | **Haversine**, обычные колонки `double precision`. PostGIS не нужен | HIGH |
| Сид-датасет | **Flyway repeatable-миграция** `R__seed_*.sql` с `ON CONFLICT DO UPDATE` | MEDIUM |
| Testcontainers | **Не брать в v1.** Вместо него `spring-boot-docker-compose` | MEDIUM |

## 1. MAX Bot API — главный риск (закрыт)

### Что подтверждено по официальным источникам

| Факт | Значение | Источник | Уверенность |
|---|---|---|---|
| Базовый домен | **`https://platform-api2.max.ru`** — не `platform-api.max.ru` | dev.max.ru/docs-api, «Обзор» | HIGH |
| Авторизация | HTTP-заголовок `Authorization: <token>`. **Передача токена через query-параметр больше не поддерживается** | dev.max.ru/docs-api | HIGH |
| TLS | Требуется сертификат Минцифры (Russian Trusted Root CA / Sub CA) в доверенных | dev.max.ru/docs-api | HIGH |
| Транспорты | **Оба**: `GET /updates` (long polling) и `POST /subscriptions` (webhook) | dev.max.ru/docs-api, раздел `subscriptions` | HIGH |
| Получение токена | `@MasterBot` в MAX → `/create` | несколько источников | MEDIUM |

### Возможности UI бота — всё, что нужно для свайпов, есть

| Возможность | Поддержка | Как | Уверенность |
|---|---|---|---|
| Изображение с подписью | **Да** | `{"text": "...", "attachments":[{"type":"image","payload":{...}}]}` | HIGH |
| **Изображение по внешнему URL (без загрузки!)** | **Да** | `image.payload.url` — `ImageAttachmentRequest.payload: {url?, token?, photos?}` | HIGH |
| Inline-клавиатура под сообщением | **Да** | `{"type":"inline_keyboard","payload":{"buttons":[[...]]}}` — двумерный массив (строки × столбцы) | HIGH |
| Картинка + клавиатура в одном сообщении | **Да** | Док прямо разрешает: «в сообщении с видео или изображением можно передать **только одно** вложение с кнопками» | HIGH |
| Callback-кнопка с payload | **Да** | `{"type":"callback","text":"👍","payload":"L:42"}` | HIGH |
| Кнопка-ссылка | **Да** | `{"type":"link","text":"Купить билет","url":"..."}`. **Лимит URL — 2048 символов** | HIGH |
| **Редактирование отправленного сообщения** | **Да** | `PUT /messages?message_id=X` с **полным телом** сообщения (text + attachments, включая новое изображение и новую клавиатуру) | HIGH |
| **Замена сообщения прямо в ответе на callback** | **Да** | `POST /answers?callback_id=X` с телом `{"message": {...}}` | HIGH |
| Удаление сообщения | Да | `DELETE /messages?message_id=X` | HIGH |
| Событие нажатия кнопки | Да | `message_callback` (`MessageCallbackUpdate`), содержит `callback_id` и `payload` | HIGH |
| Запрос геолокации кнопкой | **Да** | `{"type":"request_geo_location","text":"...","quick":true}` | HIGH |
| Запрос контакта/телефона | Да | `request_contact` + проверка `HMAC-SHA256` | HIGH |
| Форматирование текста | Да, Markdown **или** HTML (поле `format`) | — | HIGH |
| Команды бота | Да, `PATCH /bots` (`editMyCommands`) | — | HIGH |

### Лимиты — важное

| Лимит | Значение | Уверенность |
|---|---|---|
| Общий rate limit платформы | **30 запросов/сек** на бота | MEDIUM (из README SDK; в публичной доке явного числа не нашёл) |
| **Лимит на один чат** | **2 операции/сек** на `POST /messages`, `PUT /messages`, `POST /answers` | MEDIUM (README SDK; SDK его **не** контролирует) |
| Длина URL в `link`-кнопке | 2048 символов | HIGH (официальная дока) |
| Дедлайн ответа на webhook | 30 сек; затем до 10 ретраев (60с, 150с, 375с…), отписка после 8 часов неудач | MEDIUM (README SDK) |
| **Максимальный размер `payload` у callback-кнопки** | **НЕИЗВЕСТНО** — в публичной документации лимит не указан | — |

### Java SDK: что есть на самом деле

| Риск | Факт | Митигация |
|---|---|---|
| Низкая популярность | 12★, 5 форков на GitHub | Для хакатона популярность не важна; важна работоспособность |
| Один мейнтейнер | Boris Tarelkin, все коммиты его | Apache-2.0 → можно форкнуть/вендорить |
| Версия 0.x | 0.4.0 ломал API (см. раздел Migrating в README) | Зафиксировать **ровно 0.5.0**, не ставить диапазон версий |
| Не официальный | Обёртка над задокументированным REST API | Запасной путь — свой клиент на `RestClient` (~200 строк), но тогда сертификаты Минцифры придётся заводить руками |

### Long polling против webhook — берём long polling

| Критерий | Long polling | Webhook |
|---|---|---|
| Публичный HTTPS-адрес | **не нужен** | обязателен (MAX не доставляет на HTTP) |
| TLS-сертификат / reverse proxy / ngrok | нет | да |
| Работа с ноутбука за NAT | **да** | нет |
| Дедлайн обработки | нет | 30 сек, иначе ретраи и отписка |
| Отладка в docker compose | **тривиально** | нужен туннель |

## 2. Recommended Stack

### Core Technologies

| Технология | Версия | Назначение | Почему именно она | Увер. |
|---|---|---|---|---|
| Java | **21** (LTS) | Язык | Зафиксировано. SDK и Spring Boot 4 оба требуют 21+ | HIGH |
| Spring Boot | **4.1.1** | Каркас приложения | Зафиксировано. Тянет Spring Framework 7.0.9 | HIGH |
| `ru.etsft.max:max-bot-api-spring-boot` | **0.5.0** | Интеграция с MAX Bot API | Единственный живой Java SDK; собран против SB 4.1.1 | HIGH |
| `ru.etsft.max:max-bot-api-jackson` | **0.5.0** | Сериализатор для SDK | Обязателен: `MaxBotAPI.create()` ищет его в classpath. Jackson 3.1.5 = версия SB 4.1.1 | HIGH |
| PostgreSQL | **17-alpine** (образ) | БД | Подробности ниже | HIGH |
| `org.postgresql:postgresql` | **42.7.13** (managed) | JDBC-драйвер | Managed в BOM SB 4.1.1 — версию **не указывать** | HIGH |
| `spring-boot-starter-data-jpa` | 4.1.1 | Персистентность | Hibernate 7.4.5.Final + HikariCP 7.0.2 | MEDIUM |
| `spring-boot-starter-webmvc` | 4.1.1 | HTTP-контейнер | Tomcat 11.0.24. **Важно: см. готчу про daemon-потоки ниже** | MEDIUM |
| Flyway | **12.4.0** (managed) | Миграции схемы + сид | Подробности ниже | HIGH |
| Docker Compose | — | Деплой | Зафиксировано требованиями | HIGH |

### Supporting Libraries

| Библиотека | Версия | Назначение | Когда брать | Увер. |
|---|---|---|---|---|
| `spring-boot-starter-validation` | 4.1.1 | Hibernate Validator 9.1.3 | Если валидируете ввод баланса/расстояния аннотациями. Для бота часто проще `if` | MEDIUM |
| `spring-boot-starter-actuator` | 4.1.1 | `/actuator/health` | **Брать.** Нужен для `healthcheck` в docker compose, чтобы app ждал готовности БД | HIGH |
| `spring-boot-docker-compose` | 4.1.1 | Автоподъём compose при локальном запуске | Опционально, только для локальной разработки (scope `runtime`, `optional`) | MEDIUM |
| `ru.etsft.max:max-bot-api-test-support` | 0.5.0 | WireMock-стабы и JSON-фикстуры для MAX API | Если пишете тесты на слой бота — стабы уже готовы, писать нечего | MEDIUM |
| `spring-boot-configuration-processor` | 4.1.1 | Автодополнение своих `@ConfigurationProperties` | Опционально, `optional=true` | HIGH |

### Development Tools

| Инструмент | Назначение | Заметки |
|---|---|---|
| Maven Wrapper (`./mvnw`) | Сборка | Уже есть в проекте |
| `spring-boot-maven-plugin` | `build-image` / repackage | Для Dockerfile проще обычный `repackage` + multi-stage `eclipse-temurin:21-jre` |
| Overpass API (OpenStreetMap) | Разовое получение координат метро | См. раздел «Гео» — **проверено, работает** |
| WireMock 3.13.2 | Мок MAX API | Версия из `libs.versions.toml` SDK. WireMock 4.x пока только beta — **не брать** |

## 3. Installation (Maven)

### `application.yml`

## 4. База данных: PostgreSQL против H2

| Критерий | PostgreSQL | H2 |
|---|---|---|
| Требование «docker compose поднимает всё» | нативно | H2 файловый — compose-сервиса БД просто нет, требование формально не выполнено |
| Данные переживают рестарт контейнера | да (volume) | только file-mode, и это отдельная настройка |
| Диалектные отличия при переезде | — | `ON CONFLICT`, `ILIKE`, массивы, `jsonb` ведут себя иначе → переписывание SQL под дедлайн |
| Трение | `postgres:17-alpine` + 6 строк в compose | ноль на старте, боль потом |

### Spring Data JPA против Spring Data JDBC

- Схема у нас маленькая, но связная: `bot_user`, `event`, `venue`, `metro_station`, `user_event_reaction`, `user_filter`. Именно тут JPA-навигация (`event.getVenue().getLat()`) экономит писанину.
- Derived queries (`findFirstByIdNotInAndPriceLessThanEqualOrderBy…`) закрывают ленту почти без SQL.
- Подавляющее большинство знаний команды, примеров и подсказок LLM — про JPA. На дедлайне это решающий фактор.
- `spring-boot-starter-data-jpa` в 4.1.1 тянет Hibernate **7.4.5.Final** — свежий, с Jakarta Persistence 3.2.
- `spring.jpa.hibernate.ddl-auto: validate` — схему делает **только** Flyway. `update` и `create-drop` в проекте с миграциями рождают рассинхрон.
- `spring.jpa.open-in-view: false` — иначе транзакция держится весь запрос и `LazyInitializationException` прилетает не там, где вы его ждёте.
- Для ленты использовать **проекции/DTO**, а не полные сущности — карточка это 8 полей, а не граф объектов.

## 5. Миграции: Flyway

| Критерий | Flyway | Liquibase |
|---|---|---|
| Формат | Обычный `.sql` | XML/YAML/JSON/SQL — свой DSL поверх SQL |
| Порог входа | Положил `V1__init.sql` в `db/migration` — работает | Нужны changelog-файл, changeset-ы, author/id |
| Версия в BOM SB 4.1.1 | **12.4.0** | **5.0.3** |
| Rollback | Руками | Встроенный |
| Мультибазовость | Слабее | Сильнее |

## 6. Гео-расчёты: Haversine, без PostGIS

- Датасет — 100–300 мероприятий (см. PROJECT.md). Полный перебор по таблице в 300 строк с вычислением Haversine — это **доли миллисекунды**. Пространственный индекс начинает окупаться на десятках-сотнях тысяч строк.
- PostGIS — это другой docker-образ (`postgis/postgis`), `CREATE EXTENSION`, отдельный тип `geography`, Hibernate Spatial как зависимость и новый класс ошибок маппинга. На дедлайне это чистый риск без выгоды.
- pgvector здесь вообще не при чём — это про эмбеддинги, а не про географию.

### Координаты станций московского метро — где взять

| Источник | Плюсы | Минусы | Вердикт |
|---|---|---|---|
| **Overpass / OSM** | Без ключа и регистрации, сразу lat/lon, **проверено — 279 станций**, лицензия ODbL (нужна атрибуция) | Названия иногда с вариациями («ё», дефисы) | **Брать** |
| data.mos.ru, датасет №1488 «Станции Московского метрополитена» | Официальный, CSV/WKT, лицензия разрешает любое использование при указании источника | Для API нужна регистрация и ключ; CSV скачивается вручную через портал | Запасной / для сверки |
| data.mos.ru, датасет №624 «Входы и выходы вестибюлей» | Точнее — координаты конкретных вестибюлей | Избыточная детализация: нам нужна одна точка на станцию | Не нужен |
| Yandex Maps API, dadata | Удобный поиск | Ключи, лимиты, для сида избыточно | Нет |

## 7. Загрузка seed-датасета

| Вариант | Оценка |
|---|---|
| **Flyway `R__` + UPSERT** | **Рекомендуется.** Идемпотентно, версионируется в git, повторный запуск безопасен, нулевой код |
| `spring.sql.init` (`data.sql`) | Ключи в SB 4.1.1 есть, но при `ddl-auto: validate` + Flyway порядок инициализации нужно донастраивать (`spring.jpa.defer-datasource-initialization`), и идемпотентности нет из коробки. Лишнее трение |
| `CommandLineRunner` + чтение CSV | Оправдан **только** если датасет — живой CSV, который правят не-разработчики. Но тогда нужны парсер, обработка ошибок и защита от повторной вставки. Это код, которого можно не писать |
| `@Sql` в тестах | Отдельная история, для тестов — да |

## 8. Хранение секретов

# compose.yaml

- `.env` рядом с `compose.yaml`, docker compose подхватывает его автоматически. **`.env` → `.gitignore`**; в репозиторий кладётся `.env.example` с пустыми значениями.
- Синтаксис `${VAR:?сообщение}` заставляет compose упасть с внятной ошибкой, а не поднять бота с пустым токеном.
- **Relaxed binding Spring Boot работает в обе стороны:** переменная `MAX_BOT_LONGPOLLING_TOKEN` свяжется с `max.bot.longpolling.token` автоматически. Можно даже не писать плейсхолдеры в `application.yml`. Но явные `${MAX_BOT_TOKEN}` читаются понятнее — рекомендую их.
- `MAX_BOT_TOKEN` **обязательно перевыпустить**: в PROJECT.md зафиксировано, что текущий токен ушёл открытым текстом в переписку.
- Никаких `@Value` с дефолтным значением секрета в коде.

## 9. Тестирование: что оправдано на хакатоне

| Что | Брать? | Почему |
|---|---|---|
| **JUnit 5 (Jupiter 6.0.3) + AssertJ на чистую логику** | **Да, обязательно** | Haversine, «остаток баланса после покупки», фильтрация ленты (не показывать оценённое и дороже баланса) — это чистые функции. Тесты пишутся минутами, ловят настоящие баги демо. Максимальная отдача на вложенную минуту |
| **`ru.etsft.max:max-bot-api-test-support` (WireMock 3.13.2)** | Да, если останется время | Стабы и JSON-фикстуры MAX API уже написаны за вас. Дешевле, чем поднимать WireMock с нуля |
| **Testcontainers 2.0.5** | **Нет в v1** | См. ниже |
| **`@SpringBootTest` всего контекста** | Точечно, 1 «смоук» тест | Ловит ошибки конфигурации (не поднялся бин, кривая миграция). Больше одного — расточительство |
| **`@DataJpaTest`** | Нет | По умолчанию потребует embedded-БД (то есть H2, которую мы не хотим) или Testcontainers |
| **E2E против живого MAX API** | Нет | Нестабильно, тратит rate limit, требует токен в CI |

## 10. Alternatives Considered

| Рекомендовано | Альтернатива | Когда альтернатива лучше |
|---|---|---|
| `ru.etsft.max` SDK 0.5.0 | Свой клиент на `RestClient` | Если SDK окажется сломан на конкретном методе. API маленький (~31 метод), нужны 5. **Но:** придётся самим класть сертификаты Минцифры в truststore образа |
| `ru.etsft.max` SDK | Генерация клиента из OpenAPI | Публичной машиночитаемой OpenAPI-спеки MAX я **не нашёл** (в официальном TS-репозитории её нет, только рукописные `.ts`-типы). Путь закрыт |
| `ru.etsft.max` SDK | Kotlin SDK | Официального Kotlin SDK у MAX нет. Питоновые (`maxapi` от Olegt0rr/Pankovea) — не наш рантайм |
| Long polling | Webhook | Если бот переедет на VPS с доменом и TLS. Переключается свойством `max.bot.mode` |
| PostgreSQL | H2 | Никогда в этом проекте |
| Spring Data JPA | Spring Data JDBC | Если команда уже уверенно на нём и схема останется плоской |
| Flyway | Liquibase | Если нужен автоматический rollback или несколько СУБД |
| Haversine в Java | PostGIS | От десятков тысяч площадок и/или настоящих гео-запросов (полигоны, «внутри района») |
| OSM/Overpass | data.mos.ru №1488 | Если нужен именно официальный источник для отчётности/жюри |
| Flyway `R__` seed | `CommandLineRunner` + CSV | Если датасет наполняют не-разработчики прямо в проде |
| `spring-boot-docker-compose` | Testcontainers | После хакатона, когда нужен воспроизводимый CI |

## 11. What NOT to Use

| Не использовать | Почему | Вместо этого |
|---|---|---|
| `com.github.max-messenger:max-bot-api-client-java` | Артефакт JitPack по хешу коммита `3b22bc3`; исходный репозиторий **отдаёт 404**, собрать заново нельзя, обновлений не будет | `ru.etsft.max:max-bot-api-*:0.5.0` |
| `spring-boot-starter-web` | **Deprecated в Spring Boot 4** — сказано прямо в `<description>` его POM 4.1.1 | `spring-boot-starter-webmvc` |
| `platform-api.max.ru` | Официальная дока требует использовать новый домен | `platform-api2.max.ru` (дефолт в SDK) |
| Токен в query-параметре | «Передача токена через query-параметры больше не поддерживается» | Заголовок `Authorization: <token>` |
| `org.testcontainers:postgresql:2.0.5` / `:junit-jupiter:2.0.5` | Координаты переименованы в Testcontainers 2.0, старые дают **404** | `testcontainers-postgresql`, `testcontainers-junit-jupiter` — или не брать вовсе в v1 |
| `flyway-core` без `flyway-database-postgresql` | С Flyway 10+ диалекты вынесены; старт падает на `Unsupported Database` | Добавить оба артефакта |
| `spring.jpa.hibernate.ddl-auto: update` | Конфликтует с Flyway, порождает дрейф схемы, ломается тихо | `validate` + миграции |
| `spring.jpa.open-in-view: true` (дефолт) | Транзакция живёт весь запрос, `LazyInitializationException` возникает непредсказуемо | `false` |
| Jackson 2 (`com.fasterxml.jackson.*`) для DTO бота | Spring Boot 4.1.1 и SDK работают на **Jackson 3** (`tools.jackson.*`). Смешивание даёт два маппера и странные баги | Jackson 3 |
| H2 | Диалектные расхождения + не выполняет требование «compose поднимает всё» | PostgreSQL 17 |
| PostGIS / pgvector | Огромная инфраструктурная цена за 300 строк данных | Haversine |
| WireMock 4.x | На Maven Central пока только `4.0.0-beta.*` | WireMock 3.13.2 |
| Хранение состояния ленты в payload кнопки | Максимальный размер payload **не документирован**; состояние всё равно должно переживать перезапуск бота | Короткий payload `"L:42"` + состояние в БД |
| Отправка новой карточки новым сообщением | Засоряет чат, ломает ощущение «свайпа», удваивает расход rate-limit | `POST /answers` с телом `message` — замена на месте |

## 12. Stack Patterns by Variant

- `max.bot.mode=longpolling`
- Публичный адрес и TLS не нужны
- `spring-boot-docker-compose` для локальной разработки
- `max.bot.mode=webhook`, `max.bot.webhook.url`, `max.bot.webhook.secret`
- Обязательно `max.bot.webhook.async-dispatch: true` — иначе обработка с отправкой вложений упрётся в 30-секундный дедлайн
- TLS терминировать nginx-ом, приложение оставить на HTTP
- Код `UpdateHandler` **не меняется**
- Нужны ровно 5 вызовов: `POST /messages`, `PUT /messages`, `POST /answers`, `GET /updates`, `PATCH /bots`
- `RestClient` (Spring Framework 7) + `record`-DTO, ~200 строк
- **Не забыть:** добавить сертификаты Минцифры в truststore JVM внутри Dockerfile, иначе `SSLHandshakeException`
- Сначала bounding-box префильтр + btree-индексы по `lat`/`lon`
- Только потом PostGIS

## 13. Version Compatibility

| Компонент | Версия | Совместимость со Spring Boot 4.1.1 + Java 21 | Проверено как |
|---|---|---|---|
| Spring Framework | 7.0.9 | Managed | POM `spring-boot-dependencies:4.1.1` |
| Jackson | **3.1.5** (`tools.jackson`) + 2.21.5 (совместимость) | Managed | тот же POM |
| `ru.etsft.max:*` | **0.5.0** | **Собран против `springBoot = "4.1.1"`, `jackson = "3.1.5"`, `junit = "6.0.3"`** | `gradle/libs.versions.toml` репозитория SDK |
| Hibernate ORM | 7.4.5.Final | Managed | POM |
| HikariCP | 7.0.2 | Managed | POM |
| PostgreSQL JDBC | 42.7.13 | Managed | POM |
| Flyway | 12.4.0 | Managed; `flyway-database-postgresql:12.4.0` существует | POM + Maven Central |
| Liquibase | 5.0.3 | Managed (не используем) | POM |
| H2 | 2.4.240 | Managed (не используем) | POM |
| Testcontainers | 2.0.5 | Managed, **но модули переименованы** в `testcontainers-*` | `testcontainers-bom:2.0.5` |
| JUnit Jupiter | 6.0.3 | Managed — **JUnit 6**, не 5.x | POM |
| Mockito | 5.23.0 | Managed | POM |
| Tomcat | 11.0.24 | Managed | POM |
| Micrometer | 1.17.1 | Managed | POM |
| WireMock | 3.13.2 | Не managed — указывать версию явно | `libs.versions.toml` SDK |
| Java | 21 | Требование и Spring Boot 4, и SDK | — |

## 14. Что осталось неизвестным

| Вопрос | Статус | Как закрыть |
|---|---|---|
| **Максимальный размер `payload` у callback-кнопки** | В публичной документации MAX лимит не указан | Спайк на 15 минут: отправить кнопки с payload 16 / 64 / 256 / 1024 байт, посмотреть, где API вернёт ошибку. **До того — держать payload ≤ 64 байт** |
| **Как визуально ведёт себя замена изображения при `PUT /messages` / `POST /answers`** | API это разрешает (тело `EditMessageDTO` = тело `SendMessageDTO`, включая `attachments`), но перерисовку в клиентах MAX я проверить не могу | **Спайк №1 хакатона, в первый час.** Если картинка не перерисовывается на месте — запасной план: текстовая карточка с редактированием + отдельная ссылка на афишу |
| **Публичная OpenAPI-спека MAX** | Не найдена. В официальном TS-репозитории только рукописные типы | Кодогенерация как путь — закрыта |
| **Точные официальные цифры rate limit** | 30 rps и 2 ops/sec per chat взяты из README SDK, в найденном тексте официальной доки числа не встретились | Есть раздел «Рекомендации по работе с API» на dev.max.ru — прочитать при интеграции |
| **Держится ли не-web Spring Boot 4 приложение живым при long polling на виртуальных потоках** | Вывод из семантики JVM (виртуальные потоки = daemon), эмпирически не проверен | Поэтому рекомендован `spring-boot-starter-webmvc` — он снимает вопрос. Проверить за 5 минут |
| **Поведение `ru.etsft.max` SDK под нагрузкой / на граничных случаях** | 670+ юнит-тестов и живой тест-сьют есть, но реального продакшн-пробега мало (12★) | Интегрировать первым делом, не в конце |

## Sources

- https://dev.max.ru/docs-api — список методов API, домен `platform-api2.max.ru`, схема авторизации, типы кнопок, комбинации вложений, лимит URL 2048, требование сертификата Минцифры
- https://github.com/max-messenger/max-bot-api-client-ts — официальный TS-клиент (138★, обновлён 2026-09-17). Проверены файлы `types/attachment-request.ts`, `types/keyboard.ts`, `modules/messages/api.ts`, `modules/messages/types.ts` — подтвердили `image.payload.url`, `PUT /messages` с полным телом, `POST /answers` с полем `message`
- https://api.github.com/orgs/max-messenger/repos — полный список репозиториев официальной организации: Java SDK отсутствует
- https://repo1.maven.org/maven2/org/springframework/boot/spring-boot-dependencies/4.1.1/ — все managed-версии взяты напрямую из BOM
- https://repo1.maven.org/maven2/org/testcontainers/testcontainers-bom/2.0.5/ — переименование модулей Testcontainers 2.0
- Метаданные конфигурации из jar-ов `spring-boot-sql`, `spring-boot-flyway`, `spring-boot-jdbc`, `spring-boot-liquibase` 4.1.1 — подтверждено наличие `spring.sql.init.*`, `spring.flyway.*`, `spring.datasource.*`
- https://github.com/etsft/max-bot-api-java — README, `gradle/libs.versions.toml` (`springBoot = "4.1.1"`), исходники `CallbackAnswer`, `PhotoAttachmentRequestPayload`, `KeyboardBot`
- https://repo1.maven.org/maven2/ru/etsft/max/ — `maven-metadata.xml` всех шести модулей, POM-ы `max-bot-api-spring-boot` и `max-bot-api-jackson` (`tools.jackson.core:jackson-databind:3.1.5`)
- https://api.github.com/repos/etsft/max-bot-api-java — 12★, 5 форков, Apache-2.0, последний коммит 2026-09-14
- https://overpass-api.de/api/interpreter — запрос выполнен, вернул **279 станций** московского метро с координатами, без ключа
- https://data.mos.ru/datasets/1488 — «Станции Московского метрополитена», официальный запасной источник
- https://data.mos.ru/datasets/624 — «Входы и выходы вестибюлей» (избыточен)
- https://www.bytebase.com/blog/flyway-vs-liquibase/, https://www.baeldung.com/liquibase-vs-flyway, https://ankurm.com/flyway-vs-liquibase-spring-boot-4-migrations-rollbacks-baselines/ — консенсус «Flyway по умолчанию, Liquibase когда нужен rollback/мультибазовость»
- https://github.com/Olegt0rr/maxapi, https://pypi.org/project/maxapi/ — Python-библиотеки для MAX (не наш рантайм, отмечены для полноты картины)

<!-- GSD:stack-end -->

<!-- GSD:conventions-start source:CONVENTIONS.md -->

## Conventions

Conventions not yet established. Will populate as patterns emerge during development.
<!-- GSD:conventions-end -->

<!-- GSD:architecture-start source:ARCHITECTURE.md -->

## Architecture

Architecture not yet mapped. Follow existing patterns found in the codebase.
<!-- GSD:architecture-end -->

<!-- GSD:skills-start source:skills/ -->

## Project Skills

No project skills found. Add skills to any of: `.claude/skills/`, `.agents/skills/`, `.cursor/skills/`, `.github/skills/`, or `.codex/skills/` with a `SKILL.md` index file.
<!-- GSD:skills-end -->

<!-- GSD:workflow-start source:GSD defaults -->

## GSD Workflow Enforcement

Before using Edit, Write, or other file-changing tools, start work through a GSD command so planning artifacts and execution context stay in sync.

Use these entry points:

- `/gsd-quick` for small fixes, doc updates, and ad-hoc tasks
- `/gsd-debug` for investigation and bug fixing
- `/gsd-execute-phase` for planned phase work

Do not make direct repo edits outside a GSD workflow unless the user explicitly asks to bypass it.
<!-- GSD:workflow-end -->

<!-- GSD:profile-start -->

## Developer Profile

> Profile not yet configured. Run `/gsd-profile-user` to generate your developer profile.
> This section is managed by `generate-claude-profile` -- do not edit manually.
<!-- GSD:profile-end -->
