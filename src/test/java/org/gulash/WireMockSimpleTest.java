package org.gulash;

import com.github.tomakehurst.wiremock.junit5.WireMockRuntimeInfo;
import com.github.tomakehurst.wiremock.junit5.WireMockTest;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.DisplayName;

import static com.github.tomakehurst.wiremock.client.WireMock.*;
import static org.junit.jupiter.api.Assertions.*;

// @WireMockTest - это JUnit 5-расширение WireMock, которое автоматически
//  поднимает и останавливает WireMock-сервер вокруг тестов.
@WireMockTest(
    //httpPort = 8080 // Фиксирует, на каком порту WireMock будет слушать HTTP-запросы
    //,httpsEnabled = true // Включает HTTPS-режим.
    //,httpsPort = 8443 // Задаёт порт для HTTPS. Обычно используется вместе с httpsEnabled = true.
)
public class WireMockSimpleTest {

    private UserClient userClient;

    @BeforeEach
    void setUp(WireMockRuntimeInfo wmRuntimeInfo) {
        // WireMock стартует на порту случайном порту, если не используем @WireMockTest(httpPort = 8080)
        String baseUrl = wmRuntimeInfo.getHttpBaseUrl();
        userClient = new UserClient(baseUrl);
        // WireMock стартует на порту 8080 благодаря аннотации @WireMockTest(httpPort = 8080)
        //userClient = new UserClient("http://localhost:8080");
    }

    @Test
    @DisplayName("Базовый стаб для GET запроса")
    void testGetUserStub() throws Exception {
        // 1. Настраиваем заглушку (Stub)
        stubFor(get(urlEqualTo("/users/1"))
                .willReturn(aResponse()
                        .withStatus(200)
                        .withHeader("Content-Type", "application/json")
                        .withBody("{\"id\": 1, \"name\": \"John Doe\"}")));

        // 2. Вызываем клиент
        String response = userClient.getUserById("1");

        // 3. Проверяем результат
        assertEquals("{\"id\": 1, \"name\": \"John Doe\"}", response);

        // 4. Верифицируем, что WireMock действительно получил этот запрос
        verify(getRequestedFor(urlEqualTo("/users/1")));
    }

    @Test
    @DisplayName("Стаб для POST запроса с проверкой тела")
    void testCreateUserStub() throws Exception {
        stubFor(post(urlEqualTo("/users"))
                .withRequestBody(equalToJson("{\"name\": \"Alice\"}"))
                .willReturn(aResponse().withStatus(201)));

        int status = userClient.createUser("{\"name\": \"Alice\"}");

        assertEquals(201, status);
        verify(postRequestedFor(urlEqualTo("/users"))
                .withRequestBody(equalToJson("{\"name\": \"Alice\"}")));
    }

    @Test
    @DisplayName("Имитация ошибки сервера (500 Internal Server Error)")
    void testServerError() {
        stubFor(get(urlEqualTo("/users/error"))
                .willReturn(serverError()));

        Exception exception = assertThrows(RuntimeException.class, () -> {
            userClient.getUserById("error");
        });

        assertTrue(exception.getMessage().contains("500"));
    }

    @Test
    @DisplayName("Имитация задержки ответа (Timeout)")
    void testResponseDelay() throws Exception {
        stubFor(get(urlEqualTo("/users/slow"))
                .willReturn(aResponse()
                        .withStatus(200)
                        .withFixedDelay(2000))); // 2 секунды задержки

        long startTime = System.currentTimeMillis();
        userClient.getUserById("slow");
        long duration = System.currentTimeMillis() - startTime;

        assertTrue(duration >= 2000, "Запрос должен был длиться не менее 2 секунд");
    }
}
