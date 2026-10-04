package edu.cit.abella.channel;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

// VERIFY base-url against Stage 0 Postman exploration before going live -
// the manual only shows ".../tiangge/v1" as a placeholder pattern. This is
// the one property to fix if everything comes back as a connection error.
@Component
class TiangeProperties {

    @Value("${tiangge.base-url:https://legacysupply.onrender.com/tiangge/v1}")
    String baseUrl;

    @Value("${tiangge.client-id}")
    String clientId; // reuses LS_CLIENT_ID - same key as Lab 3

    @Value("${tiangge.api-key}")
    String apiKey; // reuses LS_API_KEY - same key as Lab 3

    @Value("${tiangge.app-name:abella-shop}")
    String appName;

    @Value("${tiangge.timeout-ms:3000}")
    int timeoutMs;

    @Value("${tiangge.max-attempts:3}")
    int maxAttempts;

    @Value("${tiangge.heartbeat-interval-ms:30000}")
    long heartbeatIntervalMs;

    @Value("${tiangge.feed-poll-interval-ms:5000}")
    long feedPollIntervalMs;

    @Value("${tiangge.feed-page-limit:20}")
    int feedPageLimit;
}
