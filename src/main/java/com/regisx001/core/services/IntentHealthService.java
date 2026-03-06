package com.regisx001.core.services;

import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;

import com.regisx001.core.domain.dto.IntentHealthResponse;

import lombok.extern.slf4j.Slf4j;
import reactor.core.publisher.Mono;

/**
 * Service to fetch the health status of the external Python Intent (Colab)
 * service.
 */
@Slf4j
@Service
public class IntentHealthService {

    private final WebClient colabWebClient;

    public IntentHealthService(WebClient colabWebClient) {
        this.colabWebClient = colabWebClient;
    }

    /**
     * Fetches health and system metrics (CPU, RAM, GPU) from the intent service.
     * 
     * @return a Mono emitting the health response or a fallback object if the
     *         service is unreachable
     */
    public Mono<IntentHealthResponse> fetchHealth() {
        return colabWebClient.get()
                .uri("/health")
                .retrieve()
                .bodyToMono(IntentHealthResponse.class)
                .onErrorResume(ex -> {
                    log.warn("Failed to fetch intent service health: {}", ex.getMessage());
                    return Mono.just(new IntentHealthResponse("UNREACHABLE", false, null));
                });
    }
}
