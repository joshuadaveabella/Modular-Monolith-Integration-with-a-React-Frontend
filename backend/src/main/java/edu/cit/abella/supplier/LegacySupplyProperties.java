package edu.cit.abella.supplier;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

@Component
public class LegacySupplyProperties {

    @Value("${legacysupply.base-url}")
    String baseUrl;

    @Value("${legacysupply.client-id}")
    String clientId;

    @Value("${legacysupply.api-key}")
    String apiKey;

    @Value("${legacysupply.timeout-ms:3000}")
    public int timeoutMs;

    @Value("${legacysupply.max-attempts:3}")
    int maxAttempts;
}
