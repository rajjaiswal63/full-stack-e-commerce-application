package org.ecommerce.project.controller;

import jakarta.validation.Valid;
import org.ecommerce.project.config.AppConstants;
import org.ecommerce.project.exceptions.APIException;
import org.ecommerce.project.model.Cart;
import org.ecommerce.project.payload.OrderDTO;
import org.ecommerce.project.payload.OrderRequestDTO;
import org.ecommerce.project.payload.OrderResponse;
import org.ecommerce.project.payload.OrderStatusUpdateDto;
import org.ecommerce.project.payload.RazorpayOrderDTO;
import org.ecommerce.project.payload.RazorpayPaymentConfirmationResponse;
import org.ecommerce.project.payload.RazorpayPaymentConfirmationRequest;
import org.ecommerce.project.repository.CartRepository;
import org.ecommerce.project.service.OrderService;
import org.ecommerce.project.service.RazorpayService;
import org.ecommerce.project.util.AuthUtil;
import org.ecommerce.project.util.CartFingerprint;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.Locale;

@RestController
@RequestMapping("/api")
public class OrderController {
    private final OrderService orderService;
    private final AuthUtil authUtil;

    public OrderController(OrderService orderService, AuthUtil authUtil) {
        this.orderService = orderService;
        this.authUtil = authUtil;
    }
    @PostMapping("/order/users/payments/{paymentMethod}")
    public ResponseEntity<OrderDTO> orderProducts(@PathVariable String paymentMethod,
                                                  @Valid @RequestBody OrderRequestDTO request) {
        OrderDTO order = orderService.placeOrder(authUtil.loggedInEmail(), paymentMethod, request);
        return new ResponseEntity<>(order, HttpStatus.CREATED);
    }

    @PostMapping("/order/razorpay/confirm")
    public ResponseEntity<RazorpayPaymentConfirmationResponse> confirmRazorpayPayment(
            @Valid @RequestBody RazorpayPaymentConfirmationRequest request) {
        OrderDTO order = orderService.placeRazorpayOrder(request);
        RazorpayPaymentConfirmationResponse response = new RazorpayPaymentConfirmationResponse(
                order.getOrderId(), order.getOrderStatus(), order.getTotalAmount());
        return new ResponseEntity<>(response, HttpStatus.CREATED);
    }

    @GetMapping("/order/users/orders")
    public ResponseEntity<OrderResponse> getUserOrders(
            @RequestParam(name = "pageNumber", defaultValue = AppConstants.PAGE_NUMBER) Integer pageNumber,
            @RequestParam(name = "pageSize", defaultValue = AppConstants.PAGE_SIZE) Integer pageSize,
            @RequestParam(name = "sortBy", defaultValue = AppConstants.SORT_ORDERS_BY) String sortBy,
            @RequestParam(name = "sortOrder", defaultValue = AppConstants.SORT_DIR) String sortOrder) {
        return ResponseEntity.ok(orderService.getUserOrders(pageNumber, pageSize, sortBy, sortOrder));
    }
    @PostMapping("/order/razorpay-order")
    public ResponseEntity<RazorpayOrderDTO> createRazorpayOrder(@RequestParam Long addressId) {
        return new ResponseEntity<>(orderService.createRazorpayOrder(addressId), HttpStatus.CREATED);
    }
    @GetMapping("/admin/orders")
    public ResponseEntity<OrderResponse> getAllOrders(
            @RequestParam(name = "pageNumber", defaultValue = AppConstants.PAGE_NUMBER) Integer pageNumber,
            @RequestParam(name = "pageSize", defaultValue = AppConstants.PAGE_SIZE) Integer pageSize,
            @RequestParam(name = "sortBy", defaultValue = AppConstants.SORT_ORDERS_BY) String sortBy,
            @RequestParam(name = "sortOrder", defaultValue = AppConstants.SORT_DIR) String sortOrder) {
        return ResponseEntity.ok(orderService.getAllOrders(pageNumber, pageSize, sortBy, sortOrder));
    }

    @GetMapping("/seller/orders")
    public ResponseEntity<OrderResponse> getAllSellerOrders(
            @RequestParam(name = "pageNumber", defaultValue = AppConstants.PAGE_NUMBER) Integer pageNumber,
            @RequestParam(name = "pageSize", defaultValue = AppConstants.PAGE_SIZE) Integer pageSize,
            @RequestParam(name = "sortBy", defaultValue = AppConstants.SORT_ORDERS_BY) String sortBy,
            @RequestParam(name = "sortOrder", defaultValue = AppConstants.SORT_DIR) String sortOrder) {
        return ResponseEntity.ok(orderService.getAllSellerOrders(pageNumber, pageSize, sortBy, sortOrder));
    }

    @PutMapping("/admin/orders/{orderId}/status")
    public ResponseEntity<OrderDTO> updateOrderStatus(@PathVariable Long orderId,
                                                      @Valid @RequestBody OrderStatusUpdateDto request) {
        return ResponseEntity.ok(orderService.updateOrder(orderId, request.getStatus()));
    }

    @PutMapping("/seller/orders/{orderId}/status")
    public ResponseEntity<OrderDTO> updateOrderStatusSeller(@PathVariable Long orderId,
                                                            @Valid @RequestBody OrderStatusUpdateDto request) {
        return ResponseEntity.ok(orderService.updateOrder(orderId, request.getStatus()));
    }
}