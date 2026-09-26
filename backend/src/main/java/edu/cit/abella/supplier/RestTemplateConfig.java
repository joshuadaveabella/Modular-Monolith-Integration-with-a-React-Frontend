package edu.cit.abella.supplier;

import org.springframework.boot.web.client.RestTemplateBuilder;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.client.RestTemplate;

import java.time.Duration;

@Configuration
class RestTemplateConfig {

    @Bean
    RestTemplate legacySupplyRestTemplate(RestTemplateBuilder builder, LegacySupplyProperties properties) {
        // "Time out slow calls (3 seconds or less is reasonable)" - both
        // connect and read timeouts are set from the same config value.
        return builder
                .setConnectTimeout(Duration.ofMillis(properties.timeoutMs))
                .setReadTimeout(Duration.ofMillis(properties.timeoutMs))
                .build();
    }
}