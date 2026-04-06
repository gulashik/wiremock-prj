package org.gulash;

import com.github.tomakehurst.wiremock.WireMockServer;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.io.IOException;

import static com.github.tomakehurst.wiremock.client.WireMock.*;
import static com.github.tomakehurst.wiremock.core.WireMockConfiguration.options;
import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * Пример использования WireMockServer напрямую (вместо JUnit 5 расширений).
 * Актуально, когда требуется полный контроль над жизненным циклом сервера
 * или при использовании в других тестовых фреймворках (Cucumber и т.д.).
 */
public class WireMockServerTest {

    private WireMockServer wireMockServer;
    private UserClient userClient;

    @BeforeEach
    void startServer() {
        // Конструктор с параметром 0 (или options().dynamicPort()) 
        // запускает сервер на свободном динамическом порту.
        wireMockServer = new WireMockServer(options().dynamicPort());
        wireMockServer.start();

        // Настраиваем клиент на URL запущенного сервера
        userClient = new UserClient(wireMockServer.baseUrl());
        
        // Важно: если используется WireMockServer напрямую, 
        // нужно "привязать" статические методы WireMock к этому экземпляру, 
        // либо использовать методы самого экземпляра wireMockServer.
        configureFor("localhost", wireMockServer.port());
    }

    @AfterEach
    void stopServer() {
        // Обязательная остановка сервера после тестов
        wireMockServer.stop();
    }

    @Test
    @DisplayName("Демонстрация работы с WireMockServer на динамическом порту")
    void testManualServer() throws IOException, InterruptedException {
        // Настройка стаба через статический импорт (работает благодаря configureFor)
        stubFor(get(urlEqualTo("/users/manual"))
                .willReturn(okJson("{\"id\": 10, \"name\": \"Manual User\"}")));

        // Вызов клиента
        User user = userClient.getUserById("manual");

        // Проверка
        assertEquals(10, user.getId());
        assertEquals("Manual User", user.getName());

        // Верификация через экземпляр сервера (рекомендуемый способ при ручном управлении)
        wireMockServer.verify(getRequestedFor(urlEqualTo("/users/manual")));
    }
}
