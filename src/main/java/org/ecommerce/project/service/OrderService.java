package org.ecommerce.project.service;

import org.ecommerce.project.payload.OrderDTO;
import org.ecommerce.project.payload.OrderRequestDTO;
import org.ecommerce.project.payload.OrderResponse;
import org.ecommerce.project.payload.RazorpayPaymentConfirmationRequest;
import org.ecommerce.project.payload.RazorpayOrderDTO;

public interface OrderService {
    OrderDTO placeOrder(String emailId, String paymentMethod, OrderRequestDTO request);

    OrderDTO placeRazorpayOrder(RazorpayPaymentConfirmationRequest request);

    RazorpayOrderDTO createRazorpayOrder(Long addressId);

    OrderResponse getAllOrders(Integer pageNumber, Integer pageSize, String sortBy, String sortOrder);

    OrderResponse getUserOrders(Integer pageNumber, Integer pageSize, String sortBy, String sortOrder);

    OrderResponse getAllSellerOrders(Integer pageNumber, Integer pageSize, String sortBy, String sortOrder);

    OrderDTO updateOrder(Long orderId, String status);
}