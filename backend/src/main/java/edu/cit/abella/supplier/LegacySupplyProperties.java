package edu.cit.abella.supplier;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

// The API key never has a default - if LS_API_KEY isn't set, startup should
// fail loudly rather than silently sending "null" as a credential.
@Component
class LegacySupplyProperties {

    @Value("${legacysupply.base-url}")
    String baseUrl;

    @Value("${legacysupply.client-id}")
    String clientId;

    @Value("${legacysupply.api-key}")
    String apiKey;

    @Value("${legacysupply.timeout-ms:3000}")
    int timeoutMs;

    @Value("${legacysupply.max-attempts:3}")
    int maxAttempts;
}
