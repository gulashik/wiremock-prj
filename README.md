## WireMock - это библиотека для создания локальный HTTP-заглушек, который возвращает заранее настроенные ответы (Stubs) в тестах. 

### Практическое значение:
- Тестирование без зависимости от внешних сервисов (платёжные шлюзы, погодные API, микросервисы)
- Воспроизвести редкие ошибки таймауты, Ошибки (например, 500 Internal Server Error), медленные ответы или специфические сценарии.


---

## Структура проекта

*   `build.gradle`: Конфигурация проекта, зависимости (WireMock, JUnit 5, Jackson).
*   `src/main/java/org/example/UserClient.java`: Простой HTTP-клиент (на базе `java.net.http`), который мы тестируем.
*   `src/test/java/org/example/WireMockExampleTest.java`: Набор тестов, демонстрирующих:
    *   **Stubbing (Заглушки):** Настройка ответов для GET и POST.
    *   **Verification (Верификация):** Проверка того, что клиент действительно сделал ожидаемый запрос.
    *   **Fault Injection:** Имитация ошибок сервера и задержек сети.

---

## Как запустить в IntelliJ IDEA

1.  **Открыть проект:** Выберите папку `wiremock-prj` и откройте её как проект Gradle.
2.  **Дождаться импорта:** IDEA скачает зависимости (WireMock, JUnit 5 и др.).
3.  **Запуск тестов:** 
    *   Откройте файл `src/test/java/org/example/WireMockExampleTest.java`.
    *   Нажмите на зеленый треугольник рядом с именем класса и выберите **Run 'WireMockExampleTest'**.
4.  **Просмотр результатов:** В окне "Run" внизу вы увидите результаты выполнения тестов и логи WireMock.

---

## Основные концепции WireMock в этом примере

### 1. Аннотация `@WireMockTest`
```java
@WireMockTest(httpPort = 8080)
```
Эта аннотация из расширения WireMock для JUnit 5 автоматически запускает и останавливает сервер перед/после тестов.

### 2. Создание стаба (Stubbing)
```java
stubFor(get(urlEqualTo("/users/1"))
    .willReturn(aResponse()
        .withStatus(200)
        .withBody("{\"name\": \"John\"}")));
```
Мы говорим серверу: "Если придет GET на `/users/1`, верни 200 OK с таким-то телом".

### 3. Верификация (Verification)
```java
verify(postRequestedFor(urlEqualTo("/users"))
    .withRequestBody(equalToJson("{\"name\": \"Alice\"}")));
```
Мы проверяем, что наше приложение правильно сформировало запрос к внешнему сервису.

### 4. Имитация проблем
*   `serverError()` — возвращает 500 ошибку.
*   `.withFixedDelay(2000)` — заставляет сервер ждать 2 секунды перед ответом, что идеально для тестирования таймаутов.

---

## Полезные матчеры
  
WireMock позволяет гибко настраивать условия срабатывания стабов (Stubbing) и проверки запросов (Verification):
  
- **URL матчеры:**
    - `get(urlEqualTo("/weather?city=Helsinki"))` — точное совпадение URL+params.
    - `get(urlPathEqualTo("/weather"))` — только путь (игнорирует query параметры).
    - `get(urlMatching("/weather/.*"))` — совпадение по регулярному выражению.
- **Header матчеры:**
    - `.withHeader("Authorization", matching("Bearer .*"))` — проверка по регулярному выражению.
    - `.withHeader("Content-Type", containing("json"))` — проверка на наличие подстроки.
- **Body матчеры:**
    - `.withRequestBody(equalToJson("{\"key\":\"value\"}"))` — сравнение JSON объектов (игнорирует порядок полей и пробелы).
    - `.withRequestBody(matchingJsonPath("$.name"))` — проверка, что поле существует.
    - `.withRequestBody(matchingJsonPath("$.age", equalTo("25")))` — проверка значения поля через JsonPath.
- **Query параметры:**
    - `.withQueryParam("limit", equalTo("10"))` — точное совпадение.
    - `.withQueryParam("sort", matching("asc|desc"))` — совпадение по регулярному выражению.

---

## Verify — проверка вызовов

Верификация используется для подтверждения того, что клиент действительно совершил ожидаемые вызовы к API.

```java
// Проверка, что был сделан хотя бы один запрос
verify(getRequestedFor(urlEqualTo("/users/1")));

// Проверка точного количества вызовов
verify(exactly(2), postRequestedFor(urlEqualTo("/users")));

// Проверка, что вызовов не было
verify(exactly(0), deleteRequestedFor(urlMatching("/admin/.*")));

// Проверка наличия специфичного заголовка в отправленном запросе
verify(postRequestedFor(urlEqualTo("/login"))
    .withHeader("X-Auth-Token", matching("^[A-Z0-9]+$")));
```

---

## Полезные советы (Best Practices)

*   **Используйте динамические порты:** В реальных CI/CD системах лучше использовать `@WireMockTest`, который по умолчанию выбирает свободный порт, чтобы избежать конфликтов.
*   **JSON из файлов:** Для больших ответов используйте `.withBodyFile("response.json")` (файлы должны лежать в `src/test/resources/__files`).
*   **Response Templating:** WireMock может возвращать динамические данные, основанные на входном запросе (например, подставлять ID из URL в тело ответа).