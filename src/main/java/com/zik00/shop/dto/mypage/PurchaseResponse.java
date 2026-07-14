package com.zik00.shop.dto.mypage;

import java.time.LocalDate;

import com.zik00.shop.domain.Purchase;
import lombok.Getter;

@Getter
public class PurchaseResponse {
    private final String orderNumber;
    private final String productName;
    private final int quantity;
    private final int paymentAmount;
    private final String orderStatus;
    private final LocalDate orderedDate;

    private PurchaseResponse(
            String orderNumber,
            String productName,
            int quantity,
            int paymentAmount,
            String orderStatus,
            LocalDate orderedDate
    ) {
        this.orderNumber = orderNumber;
        this.productName = productName;
        this.quantity = quantity;
        this.paymentAmount = paymentAmount;
        this.orderStatus = orderStatus;
        this.orderedDate = orderedDate;
    }

    public static PurchaseResponse from(Purchase purchase) {
        return new PurchaseResponse(
                purchase.getOrderNumber(),
                purchase.getProductName(),
                purchase.getQuantity(),
                purchase.getPaymentAmount(),
                purchase.getOrderStatus(),
                purchase.getOrderedDate()
        );
    }
}
