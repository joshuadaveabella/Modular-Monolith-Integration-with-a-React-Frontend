package edu.cit.abella.supplier;

import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestTemplate;

// The manual says sessions are "short-lived" but never says how long, and
// the assignment asks you to measure this yourself (see INTEGRATION.md).
// Rather than guess a TTL and hope it's close enough, this class holds
// whatever token it has and only re-authenticates REACTIVELY, when
// LegacySupply actually rejects it (E-AUTH-02/03/07). That makes the
// adapter correct regardless of the real session lifetime, which you
// won't know until you've measured it against your own account.
@Component
class LegacySupplySessionManager {

    private final RestTemplate restTemplate;
    private final LegacySupplyProperties properties;

    private volatile String cachedToken;

    LegacySupplySessionManager(RestTemplate restTemplate, LegacySupplyProperties properties) {
        this.restTemplate = restTemplate;
        this.properties = properties;
    }

    // Returns a token, logging in on the first call. Does not re-verify
    // an already-cached token, since the manual gives no way to check
    // liveness other than actually trying to use it.
    synchronized String currentToken() {
        if (cachedToken == null) {
            cachedToken = login();
        }
        return cachedToken;
    }

    // Called by LegacySupplyClient after an E-AUTH-02/03/07 response. Forces
    // a fresh login on the NEXT currentToken() call.
    synchronized void invalidate() {
        cachedToken = null;
    }

    private String login() {
        try {
            LsAuthRequest request = new LsAuthRequest(properties.clientId, properties.apiKey);

            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_XML);
            headers.setAccept(java.util.List.of(MediaType.APPLICATION_XML));

            HttpEntity<String> entity = new HttpEntity<>(LsXml.marshal(request), headers);

            String responseBody = restTemplate.postForObject(
                    properties.baseUrl + "/auth/token", entity, String.class);

            LsAuthResponse response = LsXml.unmarshal(responseBody, LsAuthResponse.class);
            return response.sessionToken;
        } catch (RestClientException e) {
            throw new LsCallException(LsCallException.Kind.AUTH, "E-AUTH-01",
                    "Login to LegacySupply failed: " + e.getMessage(), e);
        }
    }
}
