package org.gulash;

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

    /**
     * Использование WireMockExtension.newInstance() позволяет более гибко настраивать WireMock.
     * Например, можно использовать динамический порт (по умолчанию) и добавлять кастомные расширения.
     */
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
    @DisplayName("Проверка GET запроса с динамическим портом")
    void testGetWithExtension() throws IOException, InterruptedException {
        // В отличие от @WireMockTest, здесь мы можем использовать методы wireMock для стабов,
        // но статические импорты (stubFor) также работают, так как расширение регистрирует их.
        wireMock.stubFor(get(urlEqualTo("/users/2"))
                .willReturn(aResponse()
                        .withStatus(200)
                        .withHeader("Content-Type", "application/json")
                        .withBody("{\"id\": 2, \"name\": \"Jane Doe\"}")));

        String response = userClient.getUserById("2");

        assertEquals("{\"id\": 2, \"name\": \"Jane Doe\"}", response);
        wireMock.verify(getRequestedFor(urlEqualTo("/users/2")));
    }

    @Test
    @DisplayName("Проверка POST запроса через расширение")
    void testPostWithExtension() throws IOException, InterruptedException {
        wireMock.stubFor(post(urlEqualTo("/users"))
                .withRequestBody(equalToJson("{\"name\": \"Bob\"}"))
                .willReturn(aResponse().withStatus(201)));

        int status = userClient.createUser("{\"name\": \"Bob\"}");

        assertEquals(201, status);
        wireMock.verify(postRequestedFor(urlEqualTo("/users"))
                .withRequestBody(equalToJson("{\"name\": \"Bob\"}")));
    }

    @Test
    @DisplayName("Тестирование ошибки 404")
    void testNotFound() {
        wireMock.stubFor(get(urlEqualTo("/users/999"))
                .willReturn(notFound()));

        assertThrows(RuntimeException.class, () -> {
            userClient.getUserById("999");
        });
    }
}
