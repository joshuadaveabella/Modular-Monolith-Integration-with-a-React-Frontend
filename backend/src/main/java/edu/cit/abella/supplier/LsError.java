package edu.cit.abella.supplier;

import jakarta.xml.bind.annotation.XmlAccessType;
import jakarta.xml.bind.annotation.XmlAccessorType;
import jakarta.xml.bind.annotation.XmlElement;
import jakarta.xml.bind.annotation.XmlRootElement;

@XmlRootElement(name = "LSError")
@XmlAccessorType(XmlAccessType.FIELD)
class LsError {

    @XmlElement(name = "Code")
    String code;

    @XmlElement(name = "Message")
    String message;
}
