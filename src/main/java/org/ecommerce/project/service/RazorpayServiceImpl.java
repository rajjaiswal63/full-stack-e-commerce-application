package org.ecommerce.project.service;

import com.razorpay.Order;
import com.razorpay.Payment;
import com.razorpay.RazorpayClient;
import com.razorpay.RazorpayException;
import com.razorpay.Utils;
import org.ecommerce.project.exceptions.APIException;
import org.ecommerce.project.payload.RazorpayOrderDTO;
import org.json.JSONObject;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.util.Locale;
import java.util.UUID;
import java.util.Optional;

@Service
public class RazorpayServiceImpl implements RazorpayService {
    private final String keyId;
    private final String keySecret;

    public RazorpayServiceImpl(@Value("${razorpay.key.id:}") String keyId,
                               @Value("${razorpay.key.secret:}") String keySecret) {
        this.keyId = keyId;
        this.keySecret = keySecret;
    }

    @Override
    public RazorpayOrderDTO createOrder(long amountMinor, String currency,
                                        String ownerId, String cartFingerprint, Long addressId) {
        if (amountMinor <= 0) {
            throw new APIException("The cart total must be greater than zero.");
        }
        try {
            JSONObject request = new JSONObject();
            request.put("amount", amountMinor);
            request.put("currency", currency.toUpperCase(Locale.ROOT));
            request.put("receipt", "ecom_" + UUID.randomUUID().toString().replace("-", ""));
            request.put("notes", new JSONObject()
                    .put("owner_id", ownerId)
                    .put("cart_fingerprint", cartFingerprint)
                    .put("address_id", String.valueOf(addressId)));

            Order order = client().orders.create(request);
            return new RazorpayOrderDTO(
                    String.valueOf((Object) order.get("id")),
                    asLong(order.get("amount")),
                    String.valueOf((Object) order.get("currency")));
        } catch (RazorpayException exception) {
            throw new APIException("Unable to create a Razorpay order. Check the Razorpay keys and currency settings.");
        }
    }
    @Override
    public Optional<VerifiedRazorpayPayment> verifyCapturedPayment(String orderId, String paymentId, String signature) {
        if (isBlank(orderId) || isBlank(paymentId) || isBlank(signature)) {
            return Optional.empty();
        }
        try {
            RazorpayClient razorpay = client();
            Order order = razorpay.orders.fetch(orderId);
            long amountMinor = asLong(order.get("amount"));
            String currency = String.valueOf((Object) order.get("currency"));
            if (!orderId.equals(String.valueOf((Object) order.get("id")))) {
                return Optional.empty();
            }

            JSONObject signatureData = new JSONObject()
                    .put("razorpay_order_id", orderId)
                    .put("razorpay_payment_id", paymentId)
                    .put("razorpay_signature", signature);
            if (!Utils.verifyPaymentSignature(signatureData, keySecret)) {
                return Optional.empty();
            }

            Payment payment = razorpay.payments.fetch(paymentId);
            boolean capturedForOrder = orderId.equals(String.valueOf((Object) payment.get("order_id")))
                    && asLong(payment.get("amount")) == amountMinor
                    && currency.equalsIgnoreCase(String.valueOf((Object) payment.get("currency")))
                    && "captured".equalsIgnoreCase(String.valueOf((Object) payment.get("status")));
            if (!capturedForOrder) {
                return Optional.empty();
            }

            Object notesValue = order.get("notes");
            JSONObject notes = notesValue instanceof JSONObject jsonObject
                    ? jsonObject : new JSONObject(String.valueOf(notesValue));
            String ownerId = notes.optString("owner_id");
            String cartFingerprint = notes.optString("cart_fingerprint");
            if (isBlank(ownerId) || isBlank(cartFingerprint)) {
                return Optional.empty();
            }

            Long addressId = null;
            String addressValue = notes.optString("address_id");
            if (!isBlank(addressValue)) {
                try {
                    addressId = Long.valueOf(addressValue);
                } catch (NumberFormatException exception) {
                    return Optional.empty();
                }
            }
            return Optional.of(new VerifiedRazorpayPayment(
                    amountMinor, currency, ownerId, cartFingerprint, addressId));
        } catch (RazorpayException exception) {
            throw new APIException("Unable to verify the Razorpay payment. Please retry or contact support.");
        }
    }

    @Override
    public boolean verifyCapturedPayment(String orderId, String paymentId, String signature,
                                         long expectedAmountMinor, String currency,
                                         String ownerId, String cartFingerprint) {
        return verifyCapturedPayment(orderId, paymentId, signature)
                .filter(verified -> verified.amountMinor() == expectedAmountMinor
                        && currency.equalsIgnoreCase(verified.currency())
                        && ownerId.equals(verified.ownerId())
                        && cartFingerprint.equals(verified.cartFingerprint()))
                .isPresent();
    }
    private RazorpayClient client() throws RazorpayException {
        if (isBlank(keyId) || isBlank(keySecret)) {
            throw new APIException("Razorpay is not configured. Set RAZORPAY_KEY_ID and RAZORPAY_KEY_SECRET.");
        }
        return new RazorpayClient(keyId, keySecret);
    }

    private long asLong(Object value) {
        if (value instanceof Number number) {
            return number.longValue();
        }
        return Long.parseLong(String.valueOf(value));
    }

    private boolean isBlank(String value) {
        return value == null || value.isBlank();
    }
}