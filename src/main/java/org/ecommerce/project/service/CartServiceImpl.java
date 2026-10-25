package org.ecommerce.project.service;

import org.ecommerce.project.exceptions.APIException;
import org.ecommerce.project.exceptions.ResourceNotFoundException;
import org.ecommerce.project.model.Cart;
import org.ecommerce.project.model.CartItem;
import org.ecommerce.project.model.Product;
import org.ecommerce.project.model.User;
import org.ecommerce.project.payload.CartDTO;
import org.ecommerce.project.payload.CartItemDTO;
import org.ecommerce.project.payload.ProductDTO;
import org.ecommerce.project.repository.CartItemRepository;
import org.ecommerce.project.repository.CartRepository;
import org.ecommerce.project.repository.ProductRepository;
import org.ecommerce.project.util.AuthUtil;
import jakarta.transaction.Transactional;
import org.modelmapper.ModelMapper;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;

@Service
@Transactional
public class CartServiceImpl implements CartService {
    private final CartRepository cartRepository;
    private final CartItemRepository cartItemRepository;
    private final ProductRepository productRepository;
    private final ModelMapper modelMapper;
    private final AuthUtil authUtil;

    public CartServiceImpl(CartRepository cartRepository, CartItemRepository cartItemRepository,
                           ProductRepository productRepository, ModelMapper modelMapper, AuthUtil authUtil) {
        this.cartRepository = cartRepository;
        this.cartItemRepository = cartItemRepository;
        this.productRepository = productRepository;
        this.modelMapper = modelMapper;
        this.authUtil = authUtil;
    }

    @Override
    public CartDTO addProductToCart(Long productId, Integer quantity) {
        if (quantity == null || quantity <= 0) {
            throw new APIException("Quantity must be greater than zero.");
        }

        Cart cart = getOrCreateCurrentCart();
        Product product = getProduct(productId);
        CartItem item = cartItemRepository.findByCartIdAndProductId(cart.getCartId(), productId).orElse(null);
        int existingQuantity = item == null ? 0 : item.getQuantity();
        int requestedQuantity = existingQuantity + quantity;
        validateStock(product, requestedQuantity);

        if (item == null) {
            item = new CartItem();
            item.setCart(cart);
            item.setProduct(product);
            item.setQuantity(requestedQuantity);
        } else {
            item.setQuantity(requestedQuantity);
        }
        item.setDiscount(product.getDiscount());
        item.setProductPrice(product.getSpecialPrice());
        cart.getCartItems().removeIf(cartItem -> cartItem.getProduct().getProductId().equals(productId));
        cart.getCartItems().add(item);
        cartItemRepository.save(item);
        recalculateTotal(cart);
        return toDto(cartRepository.save(cart));
    }

    @Override
    public List<CartDTO> getAllCarts() {
        return cartRepository.findAll().stream().map(this::toDto).toList();
    }

    @Override
    public CartDTO getCart(String email, Long cartId) {
        Cart cart = cartRepository.findCartByEmailAndCartId(email, cartId);
        if (cart == null) {
            throw new ResourceNotFoundException("Cart", "cartId", cartId);
        }
        assertCartOwner(cart);
        return toDto(cart);
    }

    @Override
    public CartDTO updateProductQuantityInCart(Long productId, Integer quantityDelta) {
        if (quantityDelta == null || (quantityDelta != 1 && quantityDelta != -1)) {
            throw new APIException("Quantity change must be +1 or -1.");
        }
        Cart cart = getCurrentCart();
        CartItem item = cartItemRepository.findByCartIdAndProductId(cart.getCartId(), productId)
                .orElseThrow(() -> new ResourceNotFoundException("Cart item", "productId", productId));
        int updatedQuantity = item.getQuantity() + quantityDelta;
        if (updatedQuantity < 0) {
            throw new APIException("Cart item quantity cannot be negative.");
        }

        if (updatedQuantity == 0) {
            cartItemRepository.delete(item);
            cart.getCartItems().remove(item);
        } else {
            validateStock(item.getProduct(), updatedQuantity);
            item.setQuantity(updatedQuantity);
            item.setProductPrice(item.getProduct().getSpecialPrice());
            item.setDiscount(item.getProduct().getDiscount());
            cartItemRepository.save(item);
        }

        recalculateTotal(cart);
        return toDto(cartRepository.save(cart));
    }

    @Override
    public String deleteProductFromCart(Long cartId, Long productId) {
        Cart cart = cartRepository.findById(cartId)
                .orElseThrow(() -> new ResourceNotFoundException("Cart", "cartId", cartId));
        assertCartOwner(cart);
        CartItem item = cartItemRepository.findByCartIdAndProductId(cartId, productId)
                .orElseThrow(() -> new ResourceNotFoundException("Cart item", "productId", productId));

        cart.getCartItems().remove(item);
        cartItemRepository.delete(item);
        recalculateTotal(cart);
        cartRepository.save(cart);
        return "Product " + item.getProduct().getProductName() + " removed from the cart.";
    }

    @Override
    public void updateProductInCarts(Long cartId, Long productId) {
        Cart cart = cartRepository.findById(cartId)
                .orElseThrow(() -> new ResourceNotFoundException("Cart", "cartId", cartId));
        CartItem item = cartItemRepository.findByCartIdAndProductId(cartId, productId)
                .orElseThrow(() -> new ResourceNotFoundException("Cart item", "productId", productId));
        item.setProductPrice(item.getProduct().getSpecialPrice());
        item.setDiscount(item.getProduct().getDiscount());
        recalculateTotal(cart);
        cartItemRepository.save(item);
        cartRepository.save(cart);
    }

    @Override
    public String createOrUpdateCartWithItems(List<CartItemDTO> requestedItems) {
        if (requestedItems == null) {
            throw new APIException("Cart items are required.");
        }

        Cart cart = getOrCreateCurrentCart();
        cartItemRepository.deleteAllByCartId(cart.getCartId());
        cart.getCartItems().clear();
        cart.setTotalPrice(0.0);

        for (CartItemDTO requestedItem : requestedItems) {
            if (requestedItem == null || requestedItem.getProductId() == null
                    || requestedItem.getQuantity() == null || requestedItem.getQuantity() <= 0) {
                throw new APIException("Each cart item needs a product ID and a positive quantity.");
            }
            Product product = getProduct(requestedItem.getProductId());
            CartItem item = new CartItem();
            item.setCart(cart);
            item.setProduct(product);
            item.setQuantity(requestedItem.getQuantity());
            item.setDiscount(product.getDiscount());
            item.setProductPrice(product.getSpecialPrice());
            validateStock(product, item.getQuantity());
            cart.getCartItems().add(item);
            cartItemRepository.save(item);
        }

        recalculateTotal(cart);
        cartRepository.save(cart);
        return "Cart saved successfully.";
    }

    @Override
    public void removeProductFromAllCarts(Long productId) {
        List<Cart> carts = cartRepository.findCartsByProductId(productId);
        for (Cart cart : carts) {
            List<CartItem> items = cart.getCartItems().stream()
                    .filter(item -> item.getProduct().getProductId().equals(productId))
                    .toList();
            for (CartItem item : items) {
                cart.getCartItems().remove(item);
                cartItemRepository.delete(item);
            }
            recalculateTotal(cart);
            cartRepository.save(cart);
        }
    }
    private Cart getOrCreateCurrentCart() {
        User user = authUtil.loggedInUser();
        Cart cart = cartRepository.findByUserUserId(user.getUserId()).orElse(null);
        if (cart != null) {
            return cart;
        }
        cart = new Cart();
        cart.setUser(user);
        cart.setTotalPrice(0.0);
        return cartRepository.save(cart);
    }

    private Cart getCurrentCart() {
        Cart cart = cartRepository.findByUserUserId(authUtil.loggedInUserId())
                .orElseThrow(() -> new ResourceNotFoundException("Cart", "userId", authUtil.loggedInUserId()));
        assertCartOwner(cart);
        return cart;
    }

    private Product getProduct(Long productId) {
        return productRepository.findById(productId)
                .orElseThrow(() -> new ResourceNotFoundException("Product", "productId", productId));
    }

    private void validateStock(Product product, int quantity) {
        if (quantity <= 0) {
            throw new APIException("Quantity must be greater than zero.");
        }
        if (product.getQuantity() == null || product.getQuantity() < quantity) {
            throw new APIException("Only " + Math.max(product.getQuantity() == null ? 0 : product.getQuantity(), 0)
                    + " unit(s) of " + product.getProductName() + " are available.");
        }
    }

    private void assertCartOwner(Cart cart) {
        User currentUser = authUtil.loggedInUser();
        boolean isAdmin = org.springframework.security.core.context.SecurityContextHolder.getContext()
                .getAuthentication().getAuthorities().stream()
                .anyMatch(authority -> authority.getAuthority().equals("ROLE_ADMIN"));
        if (!isAdmin && (cart.getUser() == null || !cart.getUser().getUserId().equals(currentUser.getUserId()))) {
            throw new org.springframework.security.access.AccessDeniedException("You cannot access another user's cart.");
        }
    }

    private void recalculateTotal(Cart cart) {
        double total = cart.getCartItems().stream()
                .mapToDouble(item -> item.getProductPrice() * item.getQuantity())
                .sum();
        cart.setTotalPrice(total);
    }

    private CartDTO toDto(Cart cart) {
        CartDTO dto = modelMapper.map(cart, CartDTO.class);
        List<ProductDTO> products = new ArrayList<>();
        for (CartItem item : cart.getCartItems()) {
            ProductDTO product = modelMapper.map(item.getProduct(), ProductDTO.class);
            product.setQuantity(item.getQuantity());
            products.add(product);
        }
        dto.setProducts(products);
        return dto;
    }
}
