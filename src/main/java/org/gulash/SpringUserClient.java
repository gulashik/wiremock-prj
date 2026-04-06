package org.gulash;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

@Service
public class SpringUserClient {
    private final String baseUrl;
    private final RestTemplate restTemplate;

    public SpringUserClient(@Value("${user-service.url}") String baseUrl) {
        this.baseUrl = baseUrl;
        this.restTemplate = new RestTemplate();
    }

    public User getUserById(String id) {
        return restTemplate.getForObject(baseUrl + "/users/" + id, User.class);
    }
}
