package com.usellm.client.config;

import io.netty.channel.ChannelOption;
import io.netty.handler.timeout.ReadTimeoutHandler;
import io.netty.handler.timeout.WriteTimeoutHandler;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.client.reactive.ReactorClientHttpConnector;
import org.springframework.web.reactive.function.client.ExchangeFilterFunction;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Mono;
import reactor.netty.http.client.HttpClient;
import reactor.netty.resources.ConnectionProvider;

import java.time.Duration;
import java.util.concurrent.TimeUnit;

@Configuration
public class WebClientConfiguration {

    private static final Logger log = LoggerFactory.getLogger(WebClientConfiguration.class);

    private final LLMClientConfig config;

    public WebClientConfiguration(LLMClientConfig config) {
        this.config = config;
    }

    @Bean("llmWebClient")
    public WebClient llmWebClient() {
        ConnectionProvider provider = ConnectionProvider.builder("llm-pool")
                .maxConnections(config.getMaxConnections())
                .maxIdleTime(Duration.ofSeconds(60))
                .maxLifeTime(Duration.ofSeconds(300))
                .pendingAcquireTimeout(Duration.ofSeconds(10))
                .evictInBackground(Duration.ofSeconds(120))
                .build();

        HttpClient httpClient = HttpClient.create(provider)
                .option(ChannelOption.CONNECT_TIMEOUT_MILLIS, 10_000)
                .responseTimeout(Duration.ofSeconds(config.getTimeoutSeconds()))
                .doOnConnected(conn -> conn
                        .addHandlerLast(new ReadTimeoutHandler(config.getTimeoutSeconds(), TimeUnit.SECONDS))
                        .addHandlerLast(new WriteTimeoutHandler(30, TimeUnit.SECONDS)));

        return WebClient.builder()
                .baseUrl(config.getBaseUrl())
                .defaultHeader(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE)
                .defaultHeader(HttpHeaders.ACCEPT, MediaType.APPLICATION_JSON_VALUE)
                .defaultHeader(HttpHeaders.AUTHORIZATION, "Bearer " + config.getApiKey())
                .clientConnector(new ReactorClientHttpConnector(httpClient))
                .filter(loggingFilter())
                .codecs(configurer -> configurer.defaultCodecs().maxInMemorySize(10 * 1024 * 1024))
                .build();
    }

    private ExchangeFilterFunction loggingFilter() {
        return ExchangeFilterFunction.ofRequestProcessor(request -> {
            log.debug("LLM Request: {} {}", request.method(), request.url());
            return Mono.just(request);
        });
    }
}
