package edu.cit.abella.channel;

import edu.cit.abella.config.AppInstance;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatusCode;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.client.HttpStatusCodeException;
import org.springframework.web.client.ResourceAccessException;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestTemplate;

import java.time.Instant;
import java.util.List;

import static edu.cit.abella.channel.TiangeCallException.Kind;

// Every Tiangge endpoint path and header name lives in this one class.
// If Stage 0 probing shows a different exact path/field name than what's
// here, this is the only file that needs to change.
@Component
class TiangeHttpClient {

    private final RestTemplate restTemplate;
    private final TiangeProperties properties;
    private final AppInstance appInstance;

    TiangeHttpClient(@Qualifier("tiangeRestTemplate") RestTemplate restTemplate,
                     TiangeProperties properties, AppInstance appInstance) {
        this.restTemplate = restTemplate;
        this.properties = properties;
        this.appInstance = appInstance;
    }

    HeartbeatResponse heartbeat() {
        HeartbeatRequest body = new HeartbeatRequest(
                properties.appName, Instant.now().toString(), appInstance.uptimeSeconds());
        return exchange(HttpMethod.POST, "/instances/heartbeat", body, HeartbeatResponse.class);
    }

    void publishListings(List<ListingRequest> listings) {
        exchange(HttpMethod.PUT, "/listings", listings, Void.class);
    }

    void publishStock(List<StockUpdateRequest> updates) {
        exchange(HttpMethod.PUT, "/stock", updates, Void.class);
    }

    FeedResponse getFeed(Long after, int limit) {
        String path = "/feed?limit=" + limit + (after != null ? "&after=" + after : "");
        return exchange(HttpMethod.GET, path, null, FeedResponse.class);
    }

    void decide(String tiangeOrderId, String decision, String shopOrderId, String reason) {
        DecisionRequest body = new DecisionRequest(decision, shopOrderId, reason);
        exchange(HttpMethod.POST, "/orders/" + tiangeOrderId + "/decision", body, Void.class);
    }

    void resolve(String tiangeOrderId, String status) {
        ResolutionRequest body = new ResolutionRequest(status);
        exchange(HttpMethod.POST, "/orders/" + tiangeOrderId + "/resolution", body, Void.class);
    }

    void confirmCancellation(String tiangeOrderId, boolean restocked) {
        CancellationConfirmRequest body = new CancellationConfirmRequest(restocked);
        exchange(HttpMethod.POST, "/orders/" + tiangeOrderId + "/cancellation", body, Void.class);
    }

    private <T> T exchange(HttpMethod method, String path, Object body, Class<T> responseType) {
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        headers.setAccept(List.of(MediaType.APPLICATION_JSON));
        headers.setBearerAuth(properties.apiKey);
        headers.set("X-Client-Id", properties.clientId);
        // Task 1: every call to Tiangge AND LegacySupply carries this.
        headers.set("X-Client-Instance", appInstance.getInstanceId());

        HttpEntity<Object> entity = new HttpEntity<>(body, headers);

        try {
            return restTemplate.exchange(properties.baseUrl + path, method, entity, responseType).getBody();
        } catch (RestClientException e) {
            throw classify(e);
        }
    }

    private TiangeCallException classify(RestClientException e) {
        if (e instanceof ResourceAccessException) {
            return new TiangeCallException(Kind.NETWORK_OR_TIMEOUT, null,
                    "No response from Tiangge: " + e.getMessage(), e);
        }

        if (e instanceof HttpStatusCodeException statusEx) {
            HttpStatusCode status = statusEx.getStatusCode();
            String code = null;
            String message = e.getMessage();

            try {
                TiangeErrorResponse error = statusEx.getResponseBodyAs(TiangeErrorResponse.class);
                if (error != null) {
                    code = error.error;
                    message = error.message;
                }
            } catch (RuntimeException parseFailure) {
                // fall back to the raw message above
            }

            Kind kind = switch (status.value()) {
                case 401, 403 -> Kind.AUTH;
                case 404 -> Kind.NOT_FOUND;
                case 409 -> Kind.CONFLICT;
                case 429 -> Kind.RATE_LIMIT;
                case 400, 422 -> Kind.VALIDATION;
                case 503 -> Kind.SERVER_ERROR;
                default -> Kind.UNKNOWN;
            };

            return new TiangeCallException(kind, code, message, e);
        }

        return new TiangeCallException(Kind.UNKNOWN, null, e.getMessage(), e);
    }
}
