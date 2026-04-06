package org.gulash.springboot;

import com.github.tomakehurst.wiremock.WireMockServer;
import org.gulash.SpringUserClient;
import org.gulash.User;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.cloud.contract.wiremock.AutoConfigureWireMock;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;

import static com.github.tomakehurst.wiremock.client.WireMock.*;
import static org.junit.jupiter.api.Assertions.assertEquals;

@SpringBootTest
@AutoConfigureWireMock(port = 0) // 0 means random port
public class WireMockDynamicPortTest {

    @Autowired
    private SpringUserClient springUserClient;

    @Autowired
    private WireMockServer wireMockServer;

    @DynamicPropertySource
    static void configureProperties(DynamicPropertyRegistry registry) {
        // При использовании @AutoConfigureWireMock порт записывается в System Property
        // и становится доступен в Spring как ${wiremock.server.port}.
        registry.add("user-service.url", () -> "http://localhost:${wiremock.server.port}");
    }

    @Test
    void testWithDynamicPort() {
        // Можно использовать WireMockServer напрямую или статические методы
        wireMockServer.stubFor(get(urlEqualTo("/users/7"))
            .willReturn(aResponse()
                .withStatus(200)
                .withHeader("Content-Type", "application/json")
                .withBody("{\"id\": 7, \"name\": \"Dynamic User\"}")));

        User user = springUserClient.getUserById("7");

        assertEquals(7, user.getId());
        assertEquals("Dynamic User", user.getName());
    }
}
