package edu.cit.abella.channel;

class DecisionRequest {
    public String decision; // "ACCEPTED" | "REJECTED" | "BACKORDERED"
    public String shopOrderId;
    public String reason;

    DecisionRequest(String decision, String shopOrderId, String reason) {
        this.decision = decision;
        this.shopOrderId = shopOrderId;
        this.reason = reason;
    }
}
