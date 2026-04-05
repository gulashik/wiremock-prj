package org.gulash;

import com.github.tomakehurst.wiremock.junit5.WireMockRuntimeInfo;
import com.github.tomakehurst.wiremock.junit5.WireMockTest;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.io.IOException;

import static com.github.tomakehurst.wiremock.client.WireMock.*;
import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * Тестовый класс с демонстрацией расширенных возможностей WireMock.
 * Показывает использование различных матчеров (URL, Headers, Body) и верификации.
 */
@WireMockTest
public class WireMockAdvancedTest {

    /** Наш HTTP-клиент для тестирования. */
    private UserClient userClient;

    /**
     * Настройка перед каждым тестом.
     * @param wmRuntimeInfo Информация о WireMock сервере (порт, URL и т.д.)
     */
    @BeforeEach
    void setUp(WireMockRuntimeInfo wmRuntimeInfo) {
        userClient = new UserClient(wmRuntimeInfo.getHttpBaseUrl());
    }

    /**
     * Демонстрация различных способов проверки URL входящего запроса.
     */
    @Test
    @DisplayName("URL матчеры: urlEqualTo, urlPathEqualTo, urlMatching")
    void testUrlMatchers() {
        // 1. urlEqualTo: Проверяет полное соответствие URL, включая параметры строки запроса.
        stubFor(get(urlEqualTo("/weather?city=Helsinki"))
                .willReturn(ok()));

        // 2. urlPathEqualTo: Проверяет только путь URL, игнорируя параметры (?city=...).
        stubFor(get(urlPathEqualTo("/weather"))
                .willReturn(ok()));

        // 3. urlMatching: Использует регулярное выражение для гибкого сопоставления пути.
        stubFor(get(urlMatching("/weather/.*"))
                .willReturn(ok()));
    }

    /**
     * Проверка HTTP-заголовков в запросе.
     */
    @Test
    @DisplayName("Header матчеры: matching, containing")
    void testHeaderMatchers() {
        // stubFor вернет 200 OK только если в запросе будут нужные заголовки.
        stubFor(get(urlEqualTo("/headers"))
                .withHeader("Authorization", matching("Bearer .*")) // Соответствие регексу
                .withHeader("Content-Type", containing("json"))   // Содержит подстроку
                .willReturn(ok()));
    }

    /**
     * Проверка содержимого тела запроса (RequestBody).
     * Особенно актуально для POST/PUT запросов с JSON.
     */
    @Test
    @DisplayName("Body матчеры: equalToJson, matchingJsonPath")
    void testBodyMatchers() {
        // 1. equalToJson: Умное сравнение JSON (игнорирует пробелы и порядок полей).
        stubFor(post(urlEqualTo("/body/json"))
                .withRequestBody(equalToJson("{\"key\":\"value\"}"))
                .willReturn(ok()));

        // 2. matchingJsonPath: Проверка структуры JSON через JsonPath.
        // Проверяем, что поле "name" вообще присутствует.
        stubFor(post(urlEqualTo("/body/path"))
                .withRequestBody(matchingJsonPath("$.name"))
                .willReturn(ok()));

        // 3. matchingJsonPath: Проверка конкретного значения поля в JSON.
        stubFor(post(urlEqualTo("/body/value"))
                .withRequestBody(matchingJsonPath("$.age", equalTo("25")))
                .willReturn(ok()));
    }

    /**
     * Проверка параметров строки запроса (Query Parameters).
     */
    @Test
    @DisplayName("Query параметры: equalTo, matching")
    void testQueryParamMatchers() {
        stubFor(get(urlPathEqualTo("/search"))
                .withQueryParam("limit", equalTo("10"))
                .withQueryParam("sort", matching("asc|desc"))
                .willReturn(ok()));
    }

    /**
     * Верификация количества и факта вызовов.
     * Stubbing определяет ответ, а Verify проверяет, что вызовы действительно были сделаны.
     */
    @Test
    @DisplayName("Верификация количества вызовов (exactly, atLeast)")
    void testVerificationCounts() throws IOException, InterruptedException {
        // Настраиваем ответ
        stubFor(get(urlEqualTo("/users/1"))
                .willReturn(okJson("{\"id\": 1, \"name\": \"John\"}")));

        // Делаем два реальных запроса через клиент
        userClient.getUserById("1");
        userClient.getUserById("1");

        // Проверяем, что к WireMock пришло ровно 2 запроса по этому адресу
        verify(exactly(2), getRequestedFor(urlEqualTo("/users/1")));
        
        // Проверяем, что был сделан хотя бы один запрос
        verify(moreThanOrExactly(1), getRequestedFor(urlEqualTo("/users/1")));
    }
}
