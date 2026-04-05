package org.gulash;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.github.tomakehurst.wiremock.junit5.WireMockRuntimeInfo;
import com.github.tomakehurst.wiremock.junit5.WireMockTest;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.DisplayName;

import static com.github.tomakehurst.wiremock.client.WireMock.*;
import static org.junit.jupiter.api.Assertions.*;

// @WireMockTest - это JUnit 5-расширение WireMock, которое автоматически
//  поднимает и останавливает WireMock-сервер вокруг тестов.
// Примеры обновлены для использования Jackson.
@WireMockTest(
    //httpPort = 8080 // Фиксирует, на каком порту WireMock будет слушать HTTP-запросы
    //,httpsEnabled = true // Включает HTTPS-режим.
    //,httpsPort = 8443 // Задаёт порт для HTTPS. Обычно используется вместе с httpsEnabled = true.
)
public class WireMockSimpleTest {

    private UserClient userClient;
    private final ObjectMapper objectMapper = new ObjectMapper();

    @BeforeEach
    void setUp(WireMockRuntimeInfo wmRuntimeInfo) {
        // WireMock стартует на порту случайном порту, если не используем @WireMockTest(httpPort = 8080)
        String baseUrl = wmRuntimeInfo.getHttpBaseUrl();
        userClient = new UserClient(baseUrl);
        // WireMock стартует на порту 8080 благодаря аннотации @WireMockTest(httpPort = 8080)
        //userClient = new UserClient("http://localhost:8080");
    }

    @Test
    @DisplayName("Базовый стаб для GET запроса с использованием Jackson")
    void testGetUserStub() throws Exception {
        // Шаги теста:
        // 1. Подготовка: Создаем ожидаемого пользователя и его JSON.
        User expectedUser = new User(1, "John Doe");
        String jsonResponse = objectMapper.writeValueAsString(expectedUser);

        // 2. Настройка WireMock (Stubbing):
        //    Сообщаем WireMock: "Если придет GET на /users/1, верни JSON ответ со статусом 200".
        stubFor(get(urlEqualTo("/users/1"))
                .willReturn(aResponse()
                        .withStatus(200)
                        .withHeader("Content-Type", "application/json")
                        .withBody(jsonResponse)));

        // 3. Действие: Вызываем метод клиента.
        User response = userClient.getUserById("1");

        // 4. Проверка: Убеждаемся, что получили те данные, которые настроили в WireMock.
        assertEquals(expectedUser, response);

        // 5. Верификация: Проверяем, что запрос действительно был зафиксирован WireMock-ом.
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
