package org.ecommerce.project.payload;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class RazorpayPaymentConfirmationResponse {
    private Long orderId;
    private String orderStatus;
    private Double totalAmount;
}