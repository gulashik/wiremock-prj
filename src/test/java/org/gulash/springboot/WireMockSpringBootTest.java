package org.gulash.springboot;

import org.gulash.SpringUserClient;
import org.gulash.User;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.cloud.contract.wiremock.AutoConfigureWireMock;

import static com.github.tomakehurst.wiremock.client.WireMock.*;
import static org.junit.jupiter.api.Assertions.assertEquals;

@SpringBootTest(
    properties = {
        "user-service.url=http://localhost:${wiremock.server.port}"
    }
)
@AutoConfigureWireMock(port = 0)
public class WireMockSpringBootTest {

    @Autowired
    private SpringUserClient springUserClient;

    @Test
    void testGetUserWithSpring() {
        // Настраиваем WireMock через статический метод stubFor (доступен благодаря @AutoConfigureWireMock)
        stubFor(get(urlEqualTo("/users/42"))
            .willReturn(aResponse()
                .withStatus(200)
                .withHeader("Content-Type", "application/json")
                .withBody("{\"id\": 42, \"name\": \"Spring User\"}")));

        User user = springUserClient.getUserById("42");

        assertEquals(42, user.getId());
        assertEquals("Spring User", user.getName());
    }
}
