package org.gulash;

import com.github.tomakehurst.wiremock.junit5.WireMockExtension;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import java.io.IOException;

import static com.github.tomakehurst.wiremock.core.WireMockConfiguration.wireMockConfig;
import static org.junit.jupiter.api.Assertions.assertEquals;

public class WireMockMappingTest {

    private UserClient userClient;

    /**
     * WireMockExtension ЯВНО настроен на поиск маппингов в src/test/resources/wiremock.
     * ПО УМОЛЧАНИЮ (лучше так использовать)
     * WireMock ищет файлы в src/test/resources (для маппингов - mappings, для тел - __files).
     */
    @RegisterExtension
    static WireMockExtension wireMock = WireMockExtension.newInstance()
            .options(wireMockConfig()
                    .dynamicPort()
                    // явно настраиваем путь от src/test/resources, но имеет смысл оставить по умолчанию
                    .usingFilesUnderClasspath("wiremock")) // будет src/test/resources/wiremock/mappings
            .build();

    @BeforeEach
    void setUp() {
        String baseUrl = wireMock.baseUrl();
        userClient = new UserClient(baseUrl);
    }

    @Test
    @DisplayName("Проверка получения пользователя через JSON маппинг (файловую заглушку)")
    void testGetUserFromMapping() throws IOException, InterruptedException {
        // Шаги теста:
        // 1. Подготовка: WireMock автоматически загрузил маппинг из src/test/resources/wiremock/mappings/user-mapping.json.
        //    WireMock знает, что возвращать, так как в этом JSON файле указано соответствие:
        //    при GET запросе на URL "/users/100" нужно вернуть статус 200 и конкретное тело JSON.
        User expectedUser = new User(100, "Mapping User");

        // 2. Действие: Вызываем метод клиента, который отправляет GET запрос на /users/100.
        User response = userClient.getUserById("100");

        // 3. Проверка: Убеждаемся, что полученный от WireMock ответ совпадает с ожидаемым объектом.
        assertEquals(expectedUser, response);
    }

    @Test
    @DisplayName("Проверка получения пользователя через маппинг с использованием внешнего файла тела (bodyFileName)")
    /**
     * Тест демонстрирует использование файла 'user-200.json' из папки src/test/resources/wiremock/__files/
     * в качестве тела ответа (bodyFileName) для WireMock-заглушки.
     */
    void testGetUserFromBodyFile() throws IOException, InterruptedException {
        // Шаги теста:
        // 1. Подготовка: WireMock загрузил маппинг из src/test/resources/wiremock/mappings/user-body-file-mapping.json.
        //    WireMock сопоставляет запрос по методу GET и URL "/users/200".
        //    В маппинге указан bodyFileName: "user-200.json", поэтому WireMock берет содержимое 
        //    этого файла из папки src/test/resources/wiremock/__files/ и возвращает его в теле ответа.
        User expectedUser = new User(200, "Body File User");

        // 2. Действие: Вызываем метод клиента, который делает запрос на /users/200.
        User response = userClient.getUserById("200");

        // 3. Проверка: Сравниваем результат с ожидаемым пользователем.
        assertEquals(expectedUser, response);
    }
}
