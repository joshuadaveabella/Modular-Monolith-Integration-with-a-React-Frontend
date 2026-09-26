package edu.cit.abella.supplier;

// Wraps every way a call to LegacySupply can fail, classified by what the
// caller should do about it. This classification is what the retry logic
// in LegacySupplyClient and SupplierOrderServiceImpl actually branches on.
class LsCallException extends RuntimeException {

    enum Kind {
        NETWORK_OR_TIMEOUT, // no HTTP response at all - connect/read timeout, DNS, refused
        AUTH,               // 401 - E-AUTH-01/02/03/07
        VALIDATION,         // 400/415/422 - our request itself was malformed or wrong
        RATE_LIMIT,         // 429 - E-RATE-03
        SERVER_ERROR,       // 503 - E-SYS-50/99
        NOT_FOUND,          // 404 - E-PO-04
        IDEMPOTENCY_CONFLICT, // 409 - E-IDEM-04 (same X-Request-Id, different body)
        UNKNOWN
    }

    final Kind kind;
    final String code; // LegacySupply's error Code, e.g. "E-SYS-99", or null

    LsCallException(Kind kind, String code, String message) {
        super(message);
        this.kind = kind;
        this.code = code;
    }

    LsCallException(Kind kind, String code, String message, Throwable cause) {
        super(message, cause);
        this.kind = kind;
        this.code = code;
    }

    // NETWORK_OR_TIMEOUT, RATE_LIMIT and SERVER_ERROR are all conditions
    // that say nothing about whether our request was valid - retrying
    // makes sense. VALIDATION, NOT_FOUND and IDEMPOTENCY_CONFLICT will
    // fail identically every time, so retrying them would just waste
    // quota. AUTH is handled separately (see LegacySupplySessionManager)
    // since it triggers a re-login rather than a plain retry.
    boolean isRetryable() {
        return kind == Kind.NETWORK_OR_TIMEOUT || kind == Kind.RATE_LIMIT || kind == Kind.SERVER_ERROR;
    }
}
