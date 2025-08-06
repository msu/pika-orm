package bigsky.pika.models.chinook.beans;

import bigsky.pika.PikaORM;

public class InvoiceItemBean extends PikaORM.EnterprisePikaBean {

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

    public static PikaORM.PikaClassFinder<InvoiceItemBean> find() {
        return find(InvoiceItemBean.class);
    }
}