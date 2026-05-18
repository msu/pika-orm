package edu.montana.pika.integration.model.beans;

import edu.montana.pika.bean.EnterprisePikaBean;
import edu.montana.pika.query.PikaClassFinder;

public class InvoiceItemBean extends EnterprisePikaBean {

    int invoiceItemId;
    int invoiceId;
    int trackId;
    double unitPrice;
    int quantity;

    // Getters and setters
    public int getInvoiceItemId() {
        return invoiceItemId;
    }

    public int getInvoiceId() {
        return invoiceId;
    }

    public void setInvoiceId(int invoiceId) {
        this.invoiceId = invoiceId;
    }

    public int getTrackId() {
        return trackId;
    }

    public void setTrackId(int trackId) {
        this.trackId = trackId;
    }

    public double getUnitPrice() {
        return unitPrice;
    }

    public void setUnitPrice(double unitPrice) {
        this.unitPrice = unitPrice;
    }

    public int getQuantity() {
        return quantity;
    }

    public void setQuantity(int quantity) {
        this.quantity = quantity;
    }

    public static PikaClassFinder<InvoiceItemBean> find() {
        return find(InvoiceItemBean.class);
    }
}