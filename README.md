## WireMock - Практическое руководство по использованию

**WireMock** — это библиотека для имитации HTTP-сервисов. Она запускает локальный HTTP-сервер, который можно настроить на возврат заранее определенных ответов (стабов) на конкретные запросы.

### Зачем это нужно?
*   **Изоляция тестов:** Тестируйте свое приложение без зависимости от реальных внешних API (платежные шлюзы, социальные сети, микросервисы).
*   **Воспроизведение граничных случаев:** Легко имитировать ошибки сервера (500, 503), таймауты сети, медленные ответы или специфические JSON-структуры, которые сложно получить от реального сервиса.

---

## Структура демонстрационного проекта

*   `src/main/java/org/gulash/`:
    *   `User.java`: Простая POJO модель пользователя.
    *   `UserClient.java`: HTTP-клиент (на базе Java `HttpClient`), который тестируем.
*   `src/test/java/org/gulash/`:
    *   `WireMockSimpleTest.java`: Базовое использование аннотации `@WireMockTest`. **Рекомендуется для начала.**
    *   `WireMockAdvancedTest.java`: Примеры различных матчеров (URL, Headers, Body, Query) и верификации вызовов.
    *   `WireMockExtensionTest.java`: Программная настройка через `@RegisterExtension` (удобно для динамических портов).
    *   `WireMockServerTest.java`: Прямое управление сервером через `WireMockServer`. Полезно для сложных сценариев или других тестовых фреймворков.
    *   `WireMockMappingTest.java`: Загрузка стабов из внешних JSON-файлов (декларативный подход).
*   `src/test/resources/wiremock/`:
    *   `mappings/`: JSON-описания стабов.
    *   `__files/`: Статические файлы с телами ответов.

---

## Основные концепции на примерах

### 1. Подключение WireMock к JUnit 5
Самый простой способ — использовать аннотацию `@WireMockTest`. Она автоматически запустит сервер на свободном порту перед тестами и остановит его после.

```java
@WireMockTest
public class MyTest {
    @BeforeEach
    void setUp(WireMockRuntimeInfo wmRuntimeInfo) {
        // Получаем динамический URL запущенного сервера
        String baseUrl = wmRuntimeInfo.getHttpBaseUrl();
        client = new MyClient(baseUrl);
    }
}
```

### 2. Создание заглушки (Stubbing)
Мы приказываем серверу: "Если придет `GET` на `/users/1`, ответь статусом `200` и приложи этот JSON".

```java
stubFor(get(urlEqualTo("/users/1"))
    .willReturn(aResponse()
        .withStatus(200)
        .withHeader("Content-Type", "application/json")
        .withBody("{\"id\": 1, \"name\": \"John\"}")));
```

### 3. Проверка запросов (Verification)
Позволяет убедиться, что ваше приложение действительно сделало ожидаемый запрос с правильными параметрами.

```java
verify(postRequestedFor(urlEqualTo("/users"))
    .withRequestBody(equalToJson("{\"name\": \"Alice\"}")));
```

### 4. Имитация проблем
*   **Ошибка сервера:** `.willReturn(serverError())` (вернет 500).
*   **Задержка:** `.withFixedDelay(2000)` (сервер подождет 2 секунды перед ответом).

---

## Продвинутые возможности

### Ручное управление сервером (WireMockServer)
Если стандартные аннотации JUnit 5 не подходят, можно использовать `WireMockServer` напрямую. Это дает полный контроль над запуском и остановкой.

```java
WireMockServer wm = new WireMockServer(options().dynamicPort());
wm.start();
// Настройка и использование
wm.stubFor(get("/api").willReturn(ok()));
// ...
wm.stop();
```

### Матчеры (Matchers)
WireMock позволяет гибко описывать условия срабатывания стабов:
*   `urlPathEqualTo("/api")`: Игнорирует query-параметры.
*   `urlMatching("/users/[0-9]+")`: Регулярные выражения для URL.
*   `equalToJson("{...}")`: Сравнение JSON (игнорирует пробелы и порядок полей).
*   `matchingJsonPath("$.user.id")`: Проверка наличия поля в JSON через JsonPath.

### Внешние файлы (Mappings)
Вместо того чтобы писать код стабов в Java, их можно описать в JSON файлах в папке `mappings/`. Это делает тесты чище, а заглушки — переиспользуемыми.

Пример файла `mappings/user-mapping.json`:
```json
{
  "request": {
    "method": "GET",
    "url": "/users/100"
  },
  "response": {
    "status": 200,
    "body": "{\"id\": 100, \"name\": \"Mapping User\"}"
  }
}
```