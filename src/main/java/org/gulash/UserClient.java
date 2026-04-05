package org.gulash;

import com.fasterxml.jackson.databind.ObjectMapper;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.io.IOException;

/**
 * Простой HTTP-клиент для демонстрации работы с WireMock и Jackson.
 * Данный класс инкапсулирует логику взаимодействия с внешним API пользователей.
 */
public class UserClient {
    /** Базовый URL целевого сервиса. Передается через конструктор для гибкости в тестах. */
    private final String baseUrl;
    /** Экземпляр HttpClient для выполнения HTTP-запросов. */
    private final HttpClient httpClient;
    /** Экземпляр ObjectMapper (Jackson) для конвертации объектов в JSON и обратно. */
    private final ObjectMapper objectMapper;

    /**
     * Создает новый экземпляр клиента.
     * @param baseUrl Базовый URL внешнего сервиса (например, "http://localhost:8080")
     */
    public UserClient(String baseUrl) {
        this.baseUrl = baseUrl;
        this.httpClient = HttpClient.newHttpClient();
        this.objectMapper = new ObjectMapper();
    }

    /**
     * Получает пользователя по его идентификатору через GET-запрос.
     * @param id Строковый ID пользователя
     * @return Объект User, десериализованный из ответа сервера
     * @throws IOException При проблемах с вводом-выводом или сетью
     * @throws InterruptedException При прерывании выполнения запроса
     * @throws RuntimeException Если сервер вернул статус, отличный от 200 OK
     */
    public User getUserById(String id) throws IOException, InterruptedException {
        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(baseUrl + "/users/" + id))
                .GET()
                .build();

        HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
        
        if (response.statusCode() != 200) {
            throw new RuntimeException("Error: HTTP " + response.statusCode());
        }
        
        return objectMapper.readValue(response.body(), User.class);
    }

    /**
     * Создает нового пользователя через POST-запрос.
     * @param user Объект пользователя для отправки
     * @return HTTP статус-код ответа сервера (например, 201)
     * @throws IOException При проблемах с вводом-выводом
     * @throws InterruptedException При прерывании выполнения запроса
     */
    public int createUser(User user) throws IOException, InterruptedException {
        String json = objectMapper.writeValueAsString(user);
        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(baseUrl + "/users"))
                .header("Content-Type", "application/json")
                .POST(HttpRequest.BodyPublishers.ofString(json))
                .build();

        HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
        return response.statusCode();
    }
}
