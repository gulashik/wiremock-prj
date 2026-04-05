package org.gulash;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.github.tomakehurst.wiremock.junit5.WireMockExtension;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import java.io.IOException;

import static com.github.tomakehurst.wiremock.client.WireMock.*;
import static com.github.tomakehurst.wiremock.core.WireMockConfiguration.wireMockConfig;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

public class WireMockExtensionTest {

    private UserClient userClient;
    private final ObjectMapper objectMapper = new ObjectMapper();

    /**
     * Использование WireMockExtension.newInstance() позволяет более гибко настраивать WireMock.
     * Например, можно использовать динамический порт (по умолчанию) и добавлять кастомные расширения.
     */
    // @RegisterExtension в JUnit 5 нужен, чтобы подключить расширение как обычное поле тестового класса,
    //  а не через аннотацию на уровне класса.
    @RegisterExtension
    static WireMockExtension wireMock = WireMockExtension.newInstance()
            .options(wireMockConfig().dynamicPort()) // Динамический порт помогает избежать конфликтов
            .build();

    @BeforeEach
    void setUp() {
        // Получаем базовый URL с динамически выделенным портом
        String baseUrl = wireMock.baseUrl();
        userClient = new UserClient(baseUrl);
    }

    @Test
    @DisplayName("Проверка GET запроса с динамическим портом и Jackson")
    void testGetWithExtension() throws IOException, InterruptedException {
        // Шаги теста:
        // 1. Подготовка: Создаем ожидаемого пользователя и его JSON представление.
        User expectedUser = new User(2, "Jane Doe");
        String jsonResponse = objectMapper.writeValueAsString(expectedUser);

        // 2. Настройка WireMock (Stubbing): 
        //    Программно сообщаем WireMock, что при получении GET запроса на "/users/2" 
        //    он должен вернуть ответ с кодом 200, заголовком Content-Type и телом в формате JSON.
        wireMock.stubFor(get(urlEqualTo("/users/2"))
                .willReturn(aResponse()
                        .withStatus(200)
                        .withHeader("Content-Type", "application/json")
                        .withBody(jsonResponse)));

        // 3. Действие: Клиент отправляет запрос на сервер WireMock.
        User response = userClient.getUserById("2");

        // 4. Проверка: Убеждаемся, что клиент корректно десериализовал JSON ответ в объект User.
        assertEquals(expectedUser, response);

        // 5. Верификация: Дополнительно проверяем, что запрос действительно дошел до WireMock.
        wireMock.verify(getRequestedFor(urlEqualTo("/users/2")));
    }

    @Test
    @DisplayName("Проверка POST запроса через расширение и Jackson")
    void testPostWithExtension() throws IOException, InterruptedException {
        // Шаги теста:
        // 1. Подготовка: Создаем объект для отправки и его JSON.
        User newUser = new User(0, "Bob");
        String jsonRequest = objectMapper.writeValueAsString(newUser);

        // 2. Настройка WireMock:
        //    Указываем WireMock вернуть статус 201 (Created) только если придет POST запрос на "/users"
        //    с телом, точно соответствующим нашему JSON (используем equalToJson для гибкости форматирования).
        wireMock.stubFor(post(urlEqualTo("/users"))
                .withRequestBody(equalToJson(jsonRequest))
                .willReturn(aResponse().withStatus(201)));

        // 3. Действие: Клиент отправляет POST запрос.
        int status = userClient.createUser(newUser);

        // 4. Проверка: Убеждаемся, что вернулся статус 201.
        assertEquals(201, status);

        // 5. Верификация: Проверяем, что WireMock получил POST запрос с правильным телом.
        wireMock.verify(postRequestedFor(urlEqualTo("/users"))
                .withRequestBody(equalToJson(jsonRequest)));
    }

    @Test
    @DisplayName("Тестирование ошибки 404")
    void testNotFound() {
        // Шаги теста:
        // 1. Настройка: Просим WireMock возвращать 404 Not Found для любого GET на "/users/999".
        wireMock.stubFor(get(urlEqualTo("/users/999"))
                .willReturn(notFound()));

        // 2. Действие и Проверка: Пытаемся получить пользователя и ожидаем, что клиент выбросит RuntimeException.
        assertThrows(RuntimeException.class, () -> {
            userClient.getUserById("999");
        });
    }
}
