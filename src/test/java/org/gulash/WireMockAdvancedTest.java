package org.gulash;

import com.github.tomakehurst.wiremock.junit5.WireMockRuntimeInfo;
import com.github.tomakehurst.wiremock.junit5.WireMockTest;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.io.IOException;

import static com.github.tomakehurst.wiremock.client.WireMock.*;
import static org.junit.jupiter.api.Assertions.assertEquals;

@WireMockTest
public class WireMockAdvancedTest {

    private UserClient userClient;

    @BeforeEach
    void setUp(WireMockRuntimeInfo wmRuntimeInfo) {
        userClient = new UserClient(wmRuntimeInfo.getHttpBaseUrl());
    }

    @Test
    @DisplayName("URL матчеры: urlEqualTo, urlPathEqualTo, urlMatching")
    void testUrlMatchers() {
        // Точное совпадение URL+params
        stubFor(get(urlEqualTo("/weather?city=Helsinki"))
                .willReturn(ok()));

        // Только путь (игнорирует query параметры)
        stubFor(get(urlPathEqualTo("/weather"))
                .willReturn(ok()));

        // Регулярное выражение
        stubFor(get(urlMatching("/weather/.*"))
                .willReturn(ok()));
    }

    @Test
    @DisplayName("Header матчеры: matching, containing")
    void testHeaderMatchers() {
        stubFor(get(urlEqualTo("/headers"))
                .withHeader("Authorization", matching("Bearer .*"))
                .withHeader("Content-Type", containing("json"))
                .willReturn(ok()));
    }

    @Test
    @DisplayName("Body матчеры: equalToJson, matchingJsonPath")
    void testBodyMatchers() {
        // Сравнение JSON (игнорирует пробелы и порядок ключей)
        stubFor(post(urlEqualTo("/body/json"))
                .withRequestBody(equalToJson("{\"key\":\"value\"}"))
                .willReturn(ok()));

        // Проверка существования поля через JsonPath
        stubFor(post(urlEqualTo("/body/path"))
                .withRequestBody(matchingJsonPath("$.name"))
                .willReturn(ok()));

        // Проверка значения поля через JsonPath
        stubFor(post(urlEqualTo("/body/value"))
                .withRequestBody(matchingJsonPath("$.age", equalTo("25")))
                .willReturn(ok()));
    }

    @Test
    @DisplayName("Query параметры: equalTo, matching")
    void testQueryParamMatchers() {
        stubFor(get(urlPathEqualTo("/search"))
                .withQueryParam("limit", equalTo("10"))
                .withQueryParam("sort", matching("asc|desc"))
                .willReturn(ok()));
    }

    @Test
    @DisplayName("Верификация количества вызовов (exactly, atLeast)")
    void testVerificationCounts() throws IOException, InterruptedException {
        stubFor(get(urlEqualTo("/users/1"))
                .willReturn(okJson("{\"id\": 1, \"name\": \"John\"}")));

        userClient.getUserById("1");
        userClient.getUserById("1");

        // wireMock.verify - Проверяет факт и количество вызовов
        // Проверяем, что метод был вызван ровно 2 раза
        verify(exactly(2), getRequestedFor(urlEqualTo("/users/1")));
        
        // Проверяем, что был вызван хотя бы 1 раз
        verify(moreThanOrExactly(1), getRequestedFor(urlEqualTo("/users/1")));
        /*
            // Ровно 1 раз
            wireMock.verify(1, getRequestedFor(urlPathEqualTo("/weather")));

            // Минимум 2 раза
            wireMock.verify(moreThanOrExactly(2), getRequestedFor(urlPathEqualTo("/weather")));

            // Не вызывался
            wireMock.verify(0, postRequestedFor(urlPathEqualTo("/weather")));

            // С конкретными параметрами
            wireMock.verify(
                getRequestedFor(urlPathEqualTo("/weather"))
                    .withQueryParam("city", equalTo("Helsinki"))
                    .withHeader("Accept", equalTo("application/json"))
            );
        */
    }
}
