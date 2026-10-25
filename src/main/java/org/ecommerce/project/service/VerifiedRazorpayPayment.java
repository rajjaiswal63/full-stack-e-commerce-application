package org.ecommerce.project.service;

public record VerifiedRazorpayPayment(
        long amountMinor,
        String currency,
        String ownerId,
        String cartFingerprint,
        Long addressId) {
}