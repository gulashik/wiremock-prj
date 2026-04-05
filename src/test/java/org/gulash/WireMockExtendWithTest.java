package org.gulash;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.github.tomakehurst.wiremock.junit5.WireMockExtension;
import com.github.tomakehurst.wiremock.junit5.WireMockRuntimeInfo;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

import java.io.IOException;

import static com.github.tomakehurst.wiremock.client.WireMock.*;
import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * Использование @ExtendWith(WireMockExtension.class) — еще один способ интеграции с JUnit 5.
 * Этот способ позволяет внедрять WireMockRuntimeInfo в методы тестов или @BeforeEach,
 * что полезно для получения динамического порта.
 */
@ExtendWith(WireMockExtension.class)
public class WireMockExtendWithTest {

    private UserClient userClient;
    private final ObjectMapper objectMapper = new ObjectMapper();

    @BeforeEach
    void setUp(WireMockRuntimeInfo wmRuntimeInfo) {
        // WireMockRuntimeInfo предоставляет доступ к базовому URL и порту
        String baseUrl = wmRuntimeInfo.getHttpBaseUrl();
        userClient = new UserClient(baseUrl);
    }

    @Test
    @DisplayName("Проверка GET через @ExtendWith и внедрение WireMockRuntimeInfo с использованием Jackson")
    void testGetWithRuntimeInfo() throws IOException, InterruptedException {
        // Шаги теста:
        // 1. Подготовка: Создаем ожидаемого пользователя и JSON.
        User expectedUser = new User(3, "Charlie");
        String jsonResponse = objectMapper.writeValueAsString(expectedUser);

        // 2. Настройка WireMock:
        //    WireMock узнает, что нужно вернуть, благодаря stubFor. 
        //    Здесь мы сопоставляем GET запрос на "/users/3".
        stubFor(get(urlEqualTo("/users/3"))
                .willReturn(aResponse()
                        .withStatus(200)
                        .withHeader("Content-Type", "application/json")
                        .withBody(jsonResponse)));

        // 3. Действие: Выполняем запрос через клиента.
        User response = userClient.getUserById("3");

        // 4. Проверка: Результат должен соответствовать ожиданиям.
        assertEquals(expectedUser, response);

        // 5. Верификация: Убеждаемся, что WireMock зафиксировал вызов.
        verify(getRequestedFor(urlEqualTo("/users/3")));
    }

    @Test
    @DisplayName("Использование Jackson для POST запроса")
    void testStaticMethods() throws IOException, InterruptedException {
        // Шаги теста:
        // 1. Настройка: Программно задаем поведение WireMock для POST запроса.
        stubFor(post(urlEqualTo("/users"))
                .willReturn(aResponse().withStatus(201)));

        // 2. Действие: Отправляем данные.
        User newUser = new User(10, "New User");
        int status = userClient.createUser(newUser);

        // 3. Проверка: Статус ответа должен быть 201.
        assertEquals(201, status);

        // 4. Верификация: Проверяем, что тело запроса, которое получил WireMock,
        //    соответствует JSON представлению нашего объекта.
        verify(postRequestedFor(urlEqualTo("/users"))
                .withRequestBody(equalToJson(objectMapper.writeValueAsString(newUser))));
    }
}
