package edu.cit.abella.channel;

import org.springframework.boot.web.client.RestTemplateBuilder;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.client.RestTemplate;

import java.time.Duration;

@Configuration
class TiangeRestTemplateConfig {

    @Bean
    RestTemplate tiangeRestTemplate(RestTemplateBuilder builder, TiangeProperties properties) {
        return builder
                .setConnectTimeout(Duration.ofMillis(properties.timeoutMs))
                .setReadTimeout(Duration.ofMillis(properties.timeoutMs))
                .build();
    }
}