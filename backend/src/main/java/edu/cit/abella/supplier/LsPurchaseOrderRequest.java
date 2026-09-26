package edu.cit.abella.supplier;

import jakarta.xml.bind.annotation.XmlAccessType;
import jakarta.xml.bind.annotation.XmlAccessorType;
import jakarta.xml.bind.annotation.XmlElement;
import jakarta.xml.bind.annotation.XmlRootElement;

@XmlRootElement(name = "PurchaseOrder")
@XmlAccessorType(XmlAccessType.FIELD)
class LsPurchaseOrderRequest {

    @XmlElement(name = "SupplierSku")
    String supplierSku;

    @XmlElement(name = "Qty")
    int qty;

    @XmlElement(name = "BuyerRef")
    String buyerRef;

    LsPurchaseOrderRequest() {
    }

    LsPurchaseOrderRequest(String supplierSku, int qty, String buyerRef) {
        this.supplierSku = supplierSku;
        this.qty = qty;
        this.buyerRef = buyerRef;
    }
}
