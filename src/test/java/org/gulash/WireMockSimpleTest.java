package org.gulash;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.github.tomakehurst.wiremock.junit5.WireMockRuntimeInfo;
import com.github.tomakehurst.wiremock.junit5.WireMockTest;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.DisplayName;

import static com.github.tomakehurst.wiremock.client.WireMock.*;
import static org.junit.jupiter.api.Assertions.*;

/**
 * Тестовый класс, демонстрирующий базовое использование WireMock с аннотацией @WireMockTest.
 * Это самый простой способ интегрировать WireMock в JUnit 5 тесты.
 * Расширение автоматически управляет жизненным циклом сервера (start/stop).
 */
@WireMockTest
public class WireMockSimpleTest {

    /** Наш HTTP-клиент, который мы будем тестировать. */
    private UserClient userClient;
    /** Экземпляр Jackson ObjectMapper для подготовки JSON-данных в тестах. */
    private final ObjectMapper objectMapper = new ObjectMapper();

    /**
     * Метод настройки перед каждым тестом.
     * JUnit 5 внедряет WireMockRuntimeInfo, из которого можно получить динамический URL сервера.
     * @param wmRuntimeInfo Информация о запущенном экземпляре WireMock
     */
    @BeforeEach
    void setUp(WireMockRuntimeInfo wmRuntimeInfo) {
        // Получаем базовый URL (например, http://localhost:54321), который WireMock выделил автоматически.
        String baseUrl = wmRuntimeInfo.getHttpBaseUrl();
        userClient = new UserClient(baseUrl);
    }

    /**
     * Демонстрация создания заглушки (Stubbing) для GET-запроса.
     */
    @Test
    @DisplayName("Базовый стаб для GET запроса с использованием Jackson")
    void testGetUserStub() throws Exception {
        // 1. Подготовка: Создаем объект пользователя и преобразуем его в JSON-строку.
        User expectedUser = new User(1, "John Doe");
        String jsonResponse = objectMapper.writeValueAsString(expectedUser);

        // 2. Настройка WireMock (Stubbing):
        // stubFor - статический метод для регистрации поведения сервера.
        // get(urlEqualTo(...)) - матчер для входящего запроса.
        // aResponse() - описание того, что сервер должен вернуть.
        stubFor(get(urlEqualTo("/users/1"))
                .willReturn(aResponse()
                        .withStatus(200)
                        .withHeader("Content-Type", "application/json")
                        .withBody(jsonResponse)));

        // 3. Действие: Вызываем метод клиента, который внутри делает реальный HTTP-запрос.
        User response = userClient.getUserById("1");

        // 4. Проверка (Assertions): Убеждаемся, что клиент корректно обработал ответ.
        assertEquals(expectedUser, response);

        // 5. Верификация (Verification): Проверяем, что запрос действительно дошел до WireMock.
        // Это важно, чтобы убедиться, что клиент не использует кэш или не обращается не туда.
        verify(getRequestedFor(urlEqualTo("/users/1")));
    }

    @Test
    @DisplayName("Стаб для POST запроса с использованием Jackson")
    void testCreateUserStub() throws Exception {
        // Шаги теста:
        // 1. Подготовка: Создаем нового пользователя для отправки.
        User newUser = new User(0, "Alice");
        String jsonRequest = objectMapper.writeValueAsString(newUser);

        // 2. Настройка WireMock:
        //    WireMock вернет 201 только если получит POST на /users с телом, равным нашему JSON.
        stubFor(post(urlEqualTo("/users"))
                .withRequestBody(equalToJson(jsonRequest))
                .willReturn(aResponse().withStatus(201)));

        // 3. Действие: Выполняем создание пользователя.
        int status = userClient.createUser(newUser);

        // 4. Проверка: Статус должен быть 201 Created.
        assertEquals(201, status);

        // 5. Верификация: Проверяем детали запроса на стороне WireMock.
        verify(postRequestedFor(urlEqualTo("/users"))
                .withRequestBody(equalToJson(jsonRequest)));
    }

    @Test
    @DisplayName("Имитация ошибки сервера (500 Internal Server Error)")
    void testServerError() {
        // Шаги теста:
        // 1. Настройка: Эмулируем фатальный сбой на сервере для конкретного URL.
        stubFor(get(urlEqualTo("/users/error"))
                .willReturn(serverError()));

        // 2. Действие и Проверка: Клиент должен обработать 500 ошибку и выбросить исключение.
        Exception exception = assertThrows(RuntimeException.class, () -> {
            userClient.getUserById("error");
        });
        assertTrue(exception.getMessage().contains("500"));
    }

    @Test
    @DisplayName("Имитация задержки ответа (Timeout)")
    void testResponseDelay() throws Exception {
        // Шаги теста:
        // 1. Настройка: Добавляем фиксированную задержку (2000 мс) перед возвратом ответа.
        //    Это полезно для тестирования таймаутов на стороне клиента.
        User expectedUser = new User(1, "Slow Joe");
        stubFor(get(urlEqualTo("/users/slow"))
                .willReturn(aResponse()
                        .withStatus(200)
                        .withBody(objectMapper.writeValueAsString(expectedUser))
                        .withFixedDelay(2000))); // 2 секунды задержки

        // 2. Действие: Замеряем время выполнения запроса.
        long startTime = System.currentTimeMillis();
        userClient.getUserById("slow");
        long duration = System.currentTimeMillis() - startTime;

        // 3. Проверка: Убеждаемся, что задержка действительно произошла.
        assertTrue(duration >= 2000, "Запрос должен был длиться не менее 2 секунд");
    }
}
