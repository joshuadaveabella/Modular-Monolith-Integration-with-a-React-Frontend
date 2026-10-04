package edu.cit.abella.supplier;

import org.springframework.beans.factory.annotation.Qualifier;
import edu.cit.abella.config.AppInstance;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestTemplate;

@Component
class LegacySupplySessionManager {

    private final RestTemplate restTemplate;
    private final LegacySupplyProperties properties;
    private final AppInstance appInstance;

    private volatile String cachedToken;

    LegacySupplySessionManager(@Qualifier("legacySupplyRestTemplate") RestTemplate restTemplate,
                               LegacySupplyProperties properties,
                               AppInstance appInstance) {
        this.restTemplate = restTemplate;
        this.properties = properties;
        this.appInstance = appInstance;
    }

    synchronized String currentToken() {
        if (cachedToken == null) {
            cachedToken = login();
        }
        return cachedToken;
    }

    synchronized void invalidate() {
        cachedToken = null;
    }

    private String login() {
        try {
            LsAuthRequest request = new LsAuthRequest(properties.clientId, properties.apiKey);

            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_XML);
            headers.setAccept(java.util.List.of(MediaType.APPLICATION_XML));
            // Lab 4: "Send the same [X-Client-Instance] header on your
            // LegacySupply calls too, so both systems can tell which
            // running copy made them."
            headers.set("X-Client-Instance", appInstance.getInstanceId());

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
