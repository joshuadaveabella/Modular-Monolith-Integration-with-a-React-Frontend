package edu.cit.abella.supplier;

import jakarta.xml.bind.annotation.XmlAccessType;
import jakarta.xml.bind.annotation.XmlAccessorType;
import jakarta.xml.bind.annotation.XmlElement;

// Field set shared by PurchaseOrderAck (POST response) and
// PurchaseOrderStatus (GET response) per the manual - the only difference
// is CheckedAt, present on the tracking response.
@XmlAccessorType(XmlAccessType.FIELD)
class LsPurchaseOrderDoc {

    @XmlElement(name = "PoNumber")
    String poNumber;

    @XmlElement(name = "StatusCode")
    int statusCode;

    @XmlElement(name = "SupplierSku")
    String supplierSku;

    @XmlElement(name = "Qty")
    int qty;

    @XmlElement(name = "Uom")
    String uom;

    @XmlElement(name = "BuyerRef")
    String buyerRef;

    @XmlElement(name = "CreatedAt")
    String createdAt;

    @XmlElement(name = "CheckedAt")
    String checkedAt; // only populated on the tracking (GET) response
}
