package org.example;

import com.github.tomakehurst.wiremock.junit5.WireMockTest;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.DisplayName;

import static com.github.tomakehurst.wiremock.client.WireMock.*;
import static org.junit.jupiter.api.Assertions.*;

@WireMockTest(httpPort = 8080)
public class WireMockExampleTest {

    private UserClient userClient;

    @BeforeEach
    void setUp() {
        // WireMock автоматически стартует на порту 8080 благодаря аннотации @WireMockTest
        userClient = new UserClient("http://localhost:8080");
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
