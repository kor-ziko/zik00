package com.zik00.shop.domain;

import java.time.LocalDate;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.Table;

@Entity
@Table(name = "buylist", indexes = {
        @Index(name = "idx_buylist_member_ordered", columnList = "user_id, ordered_date, purchase_id")
})
public class Purchase {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "purchase_id")
    private Long purchaseId;

    @Column(name = "user_id")
    private long memberId;

    @Column(name = "order_number")
    private String orderNumber;

    @Column(name = "product_name")
    private String productName;

    private int quantity;

    @Column(name = "payment_amount")
    private int paymentAmount;

    @Column(name = "order_status")
    private String orderStatus;

    @Column(name = "ordered_date")
    private LocalDate orderedDate;

    protected Purchase() {
    }

    public Purchase(
            long purchaseId,
            long memberId,
            String orderNumber,
            String productName,
            int quantity,
            int paymentAmount,
            String orderStatus,
            LocalDate orderedDate
    ) {
        this.purchaseId = purchaseId > 0 ? purchaseId : null;
        this.memberId = memberId;
        this.orderNumber = orderNumber;
        this.productName = productName;
        this.quantity = quantity;
        this.paymentAmount = paymentAmount;
        this.orderStatus = orderStatus;
        this.orderedDate = orderedDate;
    }

    public long getPurchaseId() {
        return purchaseId == null ? 0L : purchaseId;
    }
    public long getMemberId() {
        return memberId;
    }
    public String getOrderNumber() {
        return orderNumber;
    }
    public String getProductName() {
        return productName;
    }
    public int getQuantity() {
        return quantity;
    }
    public int getPaymentAmount() {
        return paymentAmount;
    }
    public String getOrderStatus() {
        return orderStatus;
    }
    public LocalDate getOrderedDate() {
        return orderedDate;
    }
}
