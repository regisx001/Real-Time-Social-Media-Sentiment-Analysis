package com.regisx001.core.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.reactive.function.client.WebClient;

@Configuration
public class WebClientConfig {

    @Value("${intent.service-url}")
    private String COLAB_URL;

    @Bean
    public WebClient colabWebClient() {
        return WebClient.builder().baseUrl(COLAB_URL).build();
    }
}
