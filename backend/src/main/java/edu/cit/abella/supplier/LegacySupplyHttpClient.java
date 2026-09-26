package edu.cit.abella.supplier;

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

// The one class in this package that actually knows LegacySupply speaks
// XML over HTTP, needs a session header, and has this particular set of
// endpoints. Everything above this (SupplierOrderServiceImpl and up)
// only ever sees LsCallException and plain Java values.
@Component
class LegacySupplyHttpClient {

    private final RestTemplate restTemplate;
    private final LegacySupplySessionManager sessionManager;
    private final LegacySupplyProperties properties;

    LegacySupplyHttpClient(RestTemplate restTemplate,
                           LegacySupplySessionManager sessionManager,
                           LegacySupplyProperties properties) {
        this.restTemplate = restTemplate;
        this.sessionManager = sessionManager;
        this.properties = properties;
    }

    // Result of submitting or looking up a purchase order.
    record PoOutcome(String poNumber, int statusCode, String uom) {
    }

    /**
     * POST /purchase-orders. One HTTP attempt (retry-with-backoff is the
     * caller's job), except that a session rejection (E-AUTH-02/03/07)
     * is handled transparently here: the session is invalidated and the
     * request is sent again once with a fresh token before giving up.
     * This does not count against the caller's retry budget - a session
     * refresh isn't evidence the call itself is failing.
     */
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

    /** GET /purchase-orders/{PoNumber} */
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

    /**
     * GET /purchase-orders?buyerRef=... - used to check for an existing
     * order before (re)submitting, so a lost response never causes a
     * duplicate purchase order even if X-Request-Id de-duplication were
     * somehow bypassed.
     *
     * NOTE: the manual does not show the exact XML tag used for each
     * repeated order inside PurchaseOrderList, only that it has a Count
     * and "every order you have placed under that reference." This method
     * therefore parses defensively with XPath instead of a strict JAXB
     * class: it looks for the first PoNumber/StatusCode element anywhere
     * in the document rather than assuming one specific wrapper tag name.
     * VERIFY THIS against a real response from your own account (Part B)
     * and tighten it if you can - see INTEGRATION.md.
     */
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
        return headers;
    }

    private <T> T withSessionRetry(java.util.function.Supplier<T> call) {
        try {
            return call.get();
        } catch (LsCallException e) {
            if (e.kind == Kind.AUTH) {
                sessionManager.invalidate();
                return call.get(); // one fresh-session retry; propagates if this also fails
            }
            throw e;
        }
    }

    private LsCallException classify(RestClientException e) {
        if (e instanceof ResourceAccessException) {
            // Connect/read timeout, DNS failure, connection refused - no
            // HTTP response was ever received.
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
                // Body wasn't a parseable LSError - fall back to the raw
                // status/message. Worth a line in INTEGRATION.md if you see it.
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
