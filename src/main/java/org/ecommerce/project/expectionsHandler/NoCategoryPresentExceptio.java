package org.ecommerce.project.expectionsHandler;

public class NoCategoryPresentExceptio extends RuntimeException {
    private static long serialVersionUID = 1L;

    public NoCategoryPresentExceptio() {
    }

    public NoCategoryPresentExceptio(String message) {
        super(message);
    }
}
