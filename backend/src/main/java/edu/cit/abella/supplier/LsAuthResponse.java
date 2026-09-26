package edu.cit.abella.supplier;

import jakarta.xml.bind.annotation.XmlAccessType;
import jakarta.xml.bind.annotation.XmlAccessorType;
import jakarta.xml.bind.annotation.XmlElement;
import jakarta.xml.bind.annotation.XmlRootElement;

@XmlRootElement(name = "AuthResponse")
@XmlAccessorType(XmlAccessType.FIELD)
class LsAuthResponse {

    @XmlElement(name = "SessionToken")
    String sessionToken;

    @XmlElement(name = "IssuedAt")
    String issuedAt;
}
