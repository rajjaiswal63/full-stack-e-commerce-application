package org.ecommerce.project.service;

import org.ecommerce.project.payload.RazorpayOrderDTO;
import java.util.Optional;

public interface RazorpayService {
    RazorpayOrderDTO createOrder(long amountMinor, String currency, String ownerId, String cartFingerprint, Long addressId);

    Optional<VerifiedRazorpayPayment> verifyCapturedPayment(String orderId, String paymentId, String signature);

    boolean verifyCapturedPayment(String orderId, String paymentId, String signature,
                                  long expectedAmountMinor, String currency,
                                  String ownerId, String cartFingerprint);
}