package edu.cit.abella.channel;

class ResolutionRequest {
    public String status; // "ACCEPTED" | "CANCELLED"

    ResolutionRequest(String status) {
        this.status = status;
    }
}
