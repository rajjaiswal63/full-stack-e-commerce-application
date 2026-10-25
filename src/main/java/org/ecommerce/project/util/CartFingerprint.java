package org.ecommerce.project.util;

import org.ecommerce.project.model.Cart;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.HexFormat;
import java.util.stream.Collectors;

public final class CartFingerprint {
    private CartFingerprint() {
    }

    public static String from(Cart cart) {
        String contents = cart.getCartItems().stream()
                .map(item -> item.getProduct().getProductId() + ":" + item.getQuantity())
                .sorted()
                .collect(Collectors.joining("|"));
        try {
            byte[] digest = MessageDigest.getInstance("SHA-256")
                    .digest(contents.getBytes(StandardCharsets.UTF_8));
            return HexFormat.of().formatHex(digest);
        } catch (NoSuchAlgorithmException exception) {
            throw new IllegalStateException("SHA-256 is not available.", exception);
        }
    }
}