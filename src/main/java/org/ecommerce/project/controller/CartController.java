package org.ecommerce.project.controller;

import org.ecommerce.project.model.Cart;
import org.ecommerce.project.payload.CartDTO;
import org.ecommerce.project.payload.CartItemDTO;
import org.ecommerce.project.repository.CartRepository;
import org.ecommerce.project.service.CartService;
import org.ecommerce.project.util.AuthUtil;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api")
public class CartController {
    private final CartRepository cartRepository;
    private final AuthUtil authUtil;
    private final CartService cartService;

    public CartController(CartRepository cartRepository, AuthUtil authUtil, CartService cartService) {
        this.cartRepository = cartRepository;
        this.authUtil = authUtil;
        this.cartService = cartService;
    }

    @PostMapping("/cart/create")
    public ResponseEntity<String> createOrUpdateCart(@RequestBody List<CartItemDTO> cartItems) {
        return new ResponseEntity<>(cartService.createOrUpdateCartWithItems(cartItems), HttpStatus.CREATED);
    }

    @PostMapping("/carts/products/{productId}/quantity/{quantity}")
    public ResponseEntity<CartDTO> addProductToCart(@PathVariable Long productId,
                                                    @PathVariable Integer quantity) {
        return new ResponseEntity<>(cartService.addProductToCart(productId, quantity), HttpStatus.CREATED);
    }

    @PreAuthorize("hasRole('ADMIN')")
    @GetMapping({"/admin/carts", "/carts"})
    public ResponseEntity<List<CartDTO>> getCarts() {
        return ResponseEntity.ok(cartService.getAllCarts());
    }

    @GetMapping("/carts/users/cart")
    public ResponseEntity<CartDTO> getCartById() {
        String email = authUtil.loggedInEmail();
        Cart cart = cartRepository.findCartByEmail(email);
        if (cart == null) {
            return ResponseEntity.ok(new CartDTO());
        }
        return ResponseEntity.ok(cartService.getCart(email, cart.getCartId()));
    }

    @PutMapping("/cart/products/{productId}/quantity/{operation}")
    public ResponseEntity<CartDTO> updateCartProduct(@PathVariable Long productId,
                                                     @PathVariable String operation) {
        if (!operation.equalsIgnoreCase("add") && !operation.equalsIgnoreCase("delete")) {
            return ResponseEntity.badRequest().build();
        }
        int delta = operation.equalsIgnoreCase("delete") ? -1 : 1;
        return ResponseEntity.ok(cartService.updateProductQuantityInCart(productId, delta));
    }

    @DeleteMapping("/carts/{cartId}/product/{productId}")
    public ResponseEntity<String> deleteProductFromCart(@PathVariable Long cartId,
                                                        @PathVariable Long productId) {
        return ResponseEntity.ok(cartService.deleteProductFromCart(cartId, productId));
    }
}

