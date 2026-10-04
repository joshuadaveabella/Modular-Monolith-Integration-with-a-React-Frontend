package edu.cit.abella.supplier;

class LsCallException extends RuntimeException {

    enum Kind {
        NETWORK_OR_TIMEOUT, AUTH, VALIDATION, RATE_LIMIT, SERVER_ERROR,
        NOT_FOUND, IDEMPOTENCY_CONFLICT, UNKNOWN
    }

    final Kind kind;
    final String code;

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

    boolean isRetryable() {
        return kind == Kind.NETWORK_OR_TIMEOUT || kind == Kind.RATE_LIMIT || kind == Kind.SERVER_ERROR;
    }
}
