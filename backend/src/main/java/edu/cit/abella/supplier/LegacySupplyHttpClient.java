package edu.cit.abella.supplier;

import org.springframework.beans.factory.annotation.Qualifier;
import edu.cit.abella.config.AppInstance;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatusCode;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.client.HttpStatusCodeException;
import org.springframework.web.client.ResourceAccessException;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestTemplate;

import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.xpath.XPath;
import javax.xml.xpath.XPathConstants;
import javax.xml.xpath.XPathFactory;
import java.io.StringReader;
import java.util.List;
import java.util.Optional;

import static edu.cit.abella.supplier.LsCallException.Kind;

@Component
class LegacySupplyHttpClient {

    private final RestTemplate restTemplate;
    private final LegacySupplySessionManager sessionManager;
    private final LegacySupplyProperties properties;
    private final AppInstance appInstance;

    LegacySupplyHttpClient(@Qualifier("legacySupplyRestTemplate") RestTemplate restTemplate,
                           LegacySupplySessionManager sessionManager,
                           LegacySupplyProperties properties,
                           AppInstance appInstance) {
        this.restTemplate = restTemplate;
        this.sessionManager = sessionManager;
        this.properties = properties;
        this.appInstance = appInstance;
    }

    record PoOutcome(String poNumber, int statusCode, String uom) {
    }

    PoOutcome submitPurchaseOrder(String supplierSku, int qty, String buyerRef, String requestId) {
        return withSessionRetry(() -> {
            LsPurchaseOrderRequest body = new LsPurchaseOrderRequest(supplierSku, qty, buyerRef);
            HttpHeaders headers = xmlHeaders(sessionManager.currentToken());
            headers.set("X-Request-Id", requestId);

            HttpEntity<String> entity = new HttpEntity<>(LsXml.marshal(body), headers);

            try {
                String responseBody = restTemplate.postForObject(
                        properties.baseUrl + "/purchase-orders", entity, String.class);
                LsPurchaseOrderAck ack = LsXml.unmarshal(responseBody, LsPurchaseOrderAck.class);
                return new PoOutcome(ack.poNumber, ack.statusCode, ack.uom);
            } catch (RestClientException e) {
                throw classify(e);
            }
        });
    }

    PoOutcome getStatus(String poNumber) {
        return withSessionRetry(() -> {
            HttpHeaders headers = xmlHeaders(sessionManager.currentToken());
            HttpEntity<Void> entity = new HttpEntity<>(headers);

            try {
                var response = restTemplate.exchange(
                        properties.baseUrl + "/purchase-orders/" + poNumber,
                        org.springframework.http.HttpMethod.GET, entity, String.class);
                LsPurchaseOrderStatus status = LsXml.unmarshal(response.getBody(), LsPurchaseOrderStatus.class);
                return new PoOutcome(status.poNumber, status.statusCode, status.uom);
            } catch (RestClientException e) {
                throw classify(e);
            }
        });
    }

    Optional<PoOutcome> findExistingByBuyerRef(String buyerRef) {
        return withSessionRetry(() -> {
            HttpHeaders headers = xmlHeaders(sessionManager.currentToken());
            HttpEntity<Void> entity = new HttpEntity<>(headers);

            try {
                var response = restTemplate.exchange(
                        properties.baseUrl + "/purchase-orders?buyerRef=" + buyerRef,
                        org.springframework.http.HttpMethod.GET, entity, String.class);
                return parseFirstOrder(response.getBody());
            } catch (RestClientException e) {
                LsCallException classified = classify(e);
                if (classified.kind == Kind.NOT_FOUND) {
                    return Optional.<PoOutcome>empty();
                }
                throw classified;
            }
        });
    }

    private Optional<PoOutcome> parseFirstOrder(String xml) {
        try {
            var doc = DocumentBuilderFactory.newInstance().newDocumentBuilder()
                    .parse(new org.xml.sax.InputSource(new StringReader(xml)));
            XPath xpath = XPathFactory.newInstance().newXPath();

            String count = (String) xpath.evaluate("//Count/text()", doc, XPathConstants.STRING);
            if (count == null || count.isBlank() || "0".equals(count.trim())) {
                return Optional.empty();
            }

            String poNumber = (String) xpath.evaluate("//PoNumber[1]/text()", doc, XPathConstants.STRING);
            String statusCodeText = (String) xpath.evaluate("//StatusCode[1]/text()", doc, XPathConstants.STRING);
            String uom = (String) xpath.evaluate("//Uom[1]/text()", doc, XPathConstants.STRING);

            if (poNumber == null || poNumber.isBlank()) {
                return Optional.empty();
            }

            int statusCode = statusCodeText == null || statusCodeText.isBlank()
                    ? -1 : Integer.parseInt(statusCodeText.trim());

            return Optional.of(new PoOutcome(poNumber, statusCode, uom));
        } catch (Exception e) {
            throw new LsCallException(Kind.UNKNOWN, null,
                    "Could not parse PurchaseOrderList response: " + e.getMessage(), e);
        }
    }

    private HttpHeaders xmlHeaders(String sessionToken) {
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_XML);
        headers.setAccept(List.of(MediaType.APPLICATION_XML));
        headers.set("X-LS-Session", sessionToken);
        // Lab 4 requirement - every LegacySupply call carries the same
        // instance id used on Tiangge calls.
        headers.set("X-Client-Instance", appInstance.getInstanceId());
        return headers;
    }

    private <T> T withSessionRetry(java.util.function.Supplier<T> call) {
        try {
            return call.get();
        } catch (LsCallException e) {
            if (e.kind == Kind.AUTH) {
                sessionManager.invalidate();
                return call.get();
            }
            throw e;
        }
    }

    private LsCallException classify(RestClientException e) {
        if (e instanceof ResourceAccessException) {
            return new LsCallException(Kind.NETWORK_OR_TIMEOUT, null,
                    "No response from LegacySupply: " + e.getMessage(), e);
        }

        if (e instanceof HttpStatusCodeException statusEx) {
            HttpStatusCode status = statusEx.getStatusCode();
            String body = statusEx.getResponseBodyAsString();
            String code = null;
            String message = e.getMessage();

            try {
                LsError error = LsXml.unmarshal(body, LsError.class);
                code = error.code;
                message = error.message;
            } catch (RuntimeException parseFailure) {
                // leave code/message as the fallback above
            }

            Kind kind = switch (status.value()) {
                case 401 -> Kind.AUTH;
                case 404 -> Kind.NOT_FOUND;
                case 409 -> Kind.IDEMPOTENCY_CONFLICT;
                case 429 -> Kind.RATE_LIMIT;
                case 400, 415, 422 -> Kind.VALIDATION;
                case 503 -> Kind.SERVER_ERROR;
                default -> Kind.UNKNOWN;
            };

            return new LsCallException(kind, code, message, e);
        }

        return new LsCallException(Kind.UNKNOWN, null, e.getMessage(), e);
    }
}
