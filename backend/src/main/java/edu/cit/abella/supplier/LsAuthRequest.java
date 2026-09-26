package edu.cit.abella.supplier;

import jakarta.xml.bind.annotation.XmlAccessType;
import jakarta.xml.bind.annotation.XmlAccessorType;
import jakarta.xml.bind.annotation.XmlElement;
import jakarta.xml.bind.annotation.XmlRootElement;

@XmlRootElement(name = "AuthRequest")
@XmlAccessorType(XmlAccessType.FIELD)
class LsAuthRequest {

    @XmlElement(name = "ClientId")
    String clientId;

    @XmlElement(name = "ApiKey")
    String apiKey;

    LsAuthRequest() {
    }

    LsAuthRequest(String clientId, String apiKey) {
        this.clientId = clientId;
        this.apiKey = apiKey;
    }
}
