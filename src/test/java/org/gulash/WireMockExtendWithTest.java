package org.gulash;

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

    @BeforeEach
    void setUp(WireMockRuntimeInfo wmRuntimeInfo) {
        // WireMockRuntimeInfo предоставляет доступ к базовому URL и порту
        String baseUrl = wmRuntimeInfo.getHttpBaseUrl();
        userClient = new UserClient(baseUrl);
    }

    @Test
    @DisplayName("Проверка GET через @ExtendWith и внедрение WireMockRuntimeInfo")
    void testGetWithRuntimeInfo() throws IOException, InterruptedException {
        stubFor(get(urlEqualTo("/users/3"))
                .willReturn(aResponse()
                        .withStatus(200)
                        .withHeader("Content-Type", "application/json")
                        .withBody("{\"id\": 3, \"name\": \"Charlie\"}")));

        String response = userClient.getUserById("3");

        assertEquals("{\"id\": 3, \"name\": \"Charlie\"}", response);
        verify(getRequestedFor(urlEqualTo("/users/3")));
    }

    @Test
    @DisplayName("Использование статических методов WireMock")
    void testStaticMethods() throws IOException, InterruptedException {
        // При использовании @ExtendWith, статические методы WireMock по умолчанию привязаны к локальному серверу
        stubFor(post(urlEqualTo("/users"))
                .willReturn(aResponse().withStatus(201)));

        int status = userClient.createUser("{\"name\": \"New User\"}");

        assertEquals(201, status);
        verify(postRequestedFor(urlEqualTo("/users")));
    }
}
