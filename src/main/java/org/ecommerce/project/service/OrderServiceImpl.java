package org.ecommerce.project.service;

import org.ecommerce.project.exceptions.APIException;
import org.ecommerce.project.exceptions.ResourceNotFoundException;
import org.ecommerce.project.model.*;
import org.ecommerce.project.payload.OrderDTO;
import org.ecommerce.project.payload.OrderItemDTO;
import org.ecommerce.project.payload.OrderResponse;
import org.ecommerce.project.payload.OrderRequestDTO;
import org.ecommerce.project.payload.RazorpayPaymentConfirmationRequest;
import org.ecommerce.project.payload.RazorpayOrderDTO;
import org.ecommerce.project.repository.*;
import org.ecommerce.project.util.AuthUtil;
import org.ecommerce.project.util.CartFingerprint;
import jakarta.transaction.Transactional;
import org.modelmapper.ModelMapper;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;

@Service
@Transactional
public class OrderServiceImpl implements OrderService {
    private static final Set<String> ORDER_STATUSES = Set.of("Accepted", "Processing", "Shipped", "Delivered", "Cancelled");

    private final CartRepository cartRepository;
    private final CartItemRepository cartItemRepository;
    private final AddressRepository addressRepository;
    private final OrderItemRepository orderItemRepository;
    private final OrderRepository orderRepository;
    private final PaymentRepository paymentRepository;
    private final ProductRepository productRepository;
    private final UserRepository userRepository;
    private final ModelMapper modelMapper;
    private final AuthUtil authUtil;
    private final RazorpayService razorpayService;
    private final String razorpayCurrency;

    public OrderServiceImpl(CartRepository cartRepository, CartItemRepository cartItemRepository,
                            AddressRepository addressRepository, OrderItemRepository orderItemRepository,
                            OrderRepository orderRepository, PaymentRepository paymentRepository,
                            ProductRepository productRepository, UserRepository userRepository, ModelMapper modelMapper, AuthUtil authUtil,
                            RazorpayService razorpayService,
                            @Value("${razorpay.currency:inr}") String razorpayCurrency) {
        this.cartRepository = cartRepository;
        this.cartItemRepository = cartItemRepository;
        this.addressRepository = addressRepository;
        this.orderItemRepository = orderItemRepository;
        this.orderRepository = orderRepository;
        this.paymentRepository = paymentRepository;
        this.productRepository = productRepository;
        this.userRepository = userRepository;
        this.modelMapper = modelMapper;
        this.authUtil = authUtil;
        this.razorpayService = razorpayService;
        this.razorpayCurrency = razorpayCurrency;
    }

    @Override
    public OrderDTO placeOrder(String emailId, String paymentMethod, OrderRequestDTO request) {
        User currentUser = authUtil.loggedInUser();
        if (!currentUser.getEmail().equalsIgnoreCase(emailId)) {
            throw new AccessDeniedException("An order can only be placed for the signed-in account.");
        }
        return placeOrderForUser(currentUser, paymentMethod, request, null);
    }

    @Override
    public OrderDTO placeRazorpayOrder(RazorpayPaymentConfirmationRequest request) {
        VerifiedRazorpayPayment verifiedPayment = razorpayService.verifyCapturedPayment(
                        request.getRazorpayOrderId(), request.getRazorpayPaymentId(), request.getRazorpaySignature())
                .orElseThrow(() -> new APIException("Razorpay has not confirmed a captured payment for this order."));

        Long ownerId;
        try {
            ownerId = Long.valueOf(verifiedPayment.ownerId());
        } catch (NumberFormatException exception) {
            throw new APIException("The Razorpay order is not linked to a valid customer account.");
        }
        User orderOwner = userRepository.findById(ownerId)
                .orElseThrow(() -> new APIException("The customer account for this Razorpay order was not found."));

        Payment existingPayment = paymentRepository.findByPgPaymentId(request.getRazorpayPaymentId()).orElse(null);
        if (existingPayment != null) {
            Order existingOrder = existingPayment.getOrder();
            boolean sameVerifiedOrder = request.getRazorpayOrderId().equals(existingPayment.getPgOrderId())
                    && existingOrder != null
                    && orderOwner.getEmail().equalsIgnoreCase(existingOrder.getEmail());
            if (!sameVerifiedOrder) {
                throw new APIException("This Razorpay payment has already been used for a different order.");
            }
            OrderDTO existingOrderDto = new OrderDTO();
            existingOrderDto.setOrderId(existingOrder.getOrderId());
            existingOrderDto.setOrderStatus(existingOrder.getOrderStatus());
            existingOrderDto.setTotalAmount(existingOrder.getTotalAmount());
            return existingOrderDto;
        }

        Long addressId = verifiedPayment.addressId();
        if (addressId != null && request.getAddressId() != null && !addressId.equals(request.getAddressId())) {
            throw new APIException("The checkout address does not match the Razorpay order.");
        }
        if (addressId == null) {
            addressId = request.getAddressId();
        }
        if (addressId == null) {
            throw new APIException("The checkout address for this payment was not found. Please contact support.");
        }

        OrderRequestDTO orderRequest = new OrderRequestDTO();
        orderRequest.setAddressId(addressId);
        orderRequest.setPgName("Razorpay");
        orderRequest.setPgOrderId(request.getRazorpayOrderId());
        orderRequest.setPgPaymentId(request.getRazorpayPaymentId());
        orderRequest.setPgSignature(request.getRazorpaySignature());
        return placeOrderForUser(orderOwner, "razorpay", orderRequest, verifiedPayment);
    }

    @Override
    public RazorpayOrderDTO createRazorpayOrder(Long addressId) {
        User currentUser = authUtil.loggedInUser();
        Address address = addressRepository.findById(addressId)
                .orElseThrow(() -> new ResourceNotFoundException("Address", "addressId", addressId));
        if (address.getUser() == null || !address.getUser().getUserId().equals(currentUser.getUserId())) {
            throw new AccessDeniedException("Select an address saved to your account.");
        }

        Cart cart = cartRepository.findCartByEmail(currentUser.getEmail());
        if (cart == null || cart.getCartItems().isEmpty()) {
            throw new APIException("Add at least one product to the cart before starting payment.");
        }
        double total = cart.getCartItems().stream()
                .mapToDouble(item -> item.getProduct().getSpecialPrice() * item.getQuantity()).sum();
        long amountMinor = toMinorUnits(total);
        return razorpayService.createOrder(amountMinor, razorpayCurrency,
                String.valueOf(currentUser.getUserId()), CartFingerprint.from(cart), addressId);
    }

    private OrderDTO placeOrderForUser(User currentUser, String paymentMethod, OrderRequestDTO request,
                                       VerifiedRazorpayPayment verifiedPayment) {
        Cart cart = cartRepository.findCartByEmail(currentUser.getEmail());
        if (cart == null || cart.getCartItems().isEmpty()) {
            throw new APIException("Cart is empty.");
        }

        Address address = addressRepository.findById(request.getAddressId())
                .orElseThrow(() -> new ResourceNotFoundException("Address", "addressId", request.getAddressId()));
        if (address.getUser() == null || !address.getUser().getUserId().equals(currentUser.getUserId())) {
            throw new AccessDeniedException("Select an address saved to your account.");
        }

        double total = 0.0;
        List<CartItem> items = new ArrayList<>(cart.getCartItems());
        for (CartItem item : items) {
            Product product = item.getProduct();
            if (product.getQuantity() == null || product.getQuantity() < item.getQuantity()) {
                throw new APIException("Insufficient stock for " + product.getProductName() + ".");
            }
            item.setProductPrice(product.getSpecialPrice());
            item.setDiscount(product.getDiscount());
            total += product.getSpecialPrice() * item.getQuantity();
        }

        String normalizedMethod = paymentMethod == null ? "" : paymentMethod.trim().toLowerCase();
        Payment payment;
        if ("online".equals(normalizedMethod) || "razorpay".equals(normalizedMethod)) {
            long amountMinor = toMinorUnits(total);
            String cartFingerprint = CartFingerprint.from(cart);
            if (verifiedPayment != null) {
                boolean matchesOrder = verifiedPayment.amountMinor() == amountMinor
                        && razorpayCurrency.equalsIgnoreCase(verifiedPayment.currency())
                        && String.valueOf(currentUser.getUserId()).equals(verifiedPayment.ownerId())
                        && cartFingerprint.equals(verifiedPayment.cartFingerprint())
                        && (verifiedPayment.addressId() == null || verifiedPayment.addressId().equals(address.getAddressId()));
                if (!matchesOrder) {
                    throw new APIException("The captured Razorpay payment does not match this account, cart, address, or total.");
                }
            } else {
                boolean verified = razorpayService.verifyCapturedPayment(
                        request.getPgOrderId(), request.getPgPaymentId(), request.getPgSignature(),
                        amountMinor, razorpayCurrency, String.valueOf(currentUser.getUserId()), cartFingerprint);
                if (!verified) {
                    throw new APIException("Razorpay has not confirmed a captured payment for this cart.");
                }
            }
            if (paymentRepository.existsByPgPaymentId(request.getPgPaymentId())) {
                throw new APIException("This Razorpay payment has already been used for an order.");
            }
            payment = new Payment("Razorpay", request.getPgPaymentId(), "captured",
                    "Payment captured and verified by Razorpay", "Razorpay");
            payment.setPgOrderId(request.getPgOrderId());
        } else if ("cod".equals(normalizedMethod) || "cash on delivery".equals(normalizedMethod)) {
            payment = new Payment("Cash on delivery", null, "pending", "Payment due on delivery", "Cash on delivery");
        } else {
            throw new APIException("Supported payment methods are Razorpay and cash on delivery.");
        }

        Order order = new Order();
        order.setEmail(currentUser.getEmail());
        order.setOrderDate(LocalDate.now());
        order.setTotalAmount(total);
        order.setOrderStatus("Accepted");
        order.setAddress(address);

        Payment savedPayment = paymentRepository.save(payment);
        order.setPayment(savedPayment);
        Order savedOrder = orderRepository.save(order);
        savedPayment.setOrder(savedOrder);
        paymentRepository.save(savedPayment);

        List<OrderItem> orderItems = new ArrayList<>();
        for (CartItem item : items) {
            Product product = item.getProduct();
            OrderItem orderItem = new OrderItem();
            orderItem.setProduct(product);
            orderItem.setQuantity(item.getQuantity());
            orderItem.setDiscount(item.getDiscount());
            orderItem.setOrderedProductPrice(item.getProductPrice());
            orderItem.setOrder(savedOrder);
            orderItems.add(orderItem);

            product.setQuantity(product.getQuantity() - item.getQuantity());
            productRepository.save(product);
        }
        List<OrderItem> savedItems = orderItemRepository.saveAll(orderItems);

        // Cart.cartItems uses orphanRemoval=true, so clearing the managed collection deletes each row once.
        cart.getCartItems().clear();
        cart.setTotalPrice(0.0);
        cartRepository.save(cart);

        OrderDTO dto = modelMapper.map(savedOrder, OrderDTO.class);
        dto.setAddressId(address.getAddressId());
        dto.setOrderItems(savedItems.stream()
                .map(item -> modelMapper.map(item, OrderItemDTO.class)).toList());
        return dto;
    }
    @Override
    @Transactional(Transactional.TxType.SUPPORTS)
    public OrderResponse getAllOrders(Integer pageNumber, Integer pageSize, String sortBy, String sortOrder) {
        validatePaging(pageNumber, pageSize);
        Set<String> allowedSorts = Set.of("orderId", "orderDate", "totalAmount", "orderStatus");
        String sortField = sortBy != null && allowedSorts.contains(sortBy) ? sortBy : "orderDate";
        Sort sort = "asc".equalsIgnoreCase(sortOrder) ? Sort.by(sortField).ascending() : Sort.by(sortField).descending();
        return toResponse(orderRepository.findAll(PageRequest.of(pageNumber, pageSize, sort)));
    }

    @Override
    @Transactional(Transactional.TxType.SUPPORTS)
    public OrderResponse getUserOrders(Integer pageNumber, Integer pageSize, String sortBy, String sortOrder) {
        validatePaging(pageNumber, pageSize);
        Set<String> allowedSorts = Set.of("orderId", "orderDate", "totalAmount", "orderStatus");
        String sortField = sortBy != null && allowedSorts.contains(sortBy) ? sortBy : "orderDate";
        Sort sort = "asc".equalsIgnoreCase(sortOrder) ? Sort.by(sortField).ascending() : Sort.by(sortField).descending();
        Pageable page = PageRequest.of(pageNumber, pageSize, sort);
        return toResponse(orderRepository.findByEmailIgnoreCase(authUtil.loggedInEmail(), page));
    }
    @Override
    public OrderDTO updateOrder(Long orderId, String status) {
        Order order = orderRepository.findById(orderId)
                .orElseThrow(() -> new ResourceNotFoundException("Order", "orderId", orderId));
        String normalizedStatus = normalizeStatus(status);
        if (!ORDER_STATUSES.contains(normalizedStatus)) {
            throw new APIException("Status must be Accepted, Processing, Shipped, Delivered, or Cancelled.");
        }

        boolean isAdmin = isAdmin();
        if (!isAdmin) {
            Long sellerId = authUtil.loggedInUserId();
            boolean ownsOrder = order.getOrderItems().stream()
                    .map(OrderItem::getProduct)
                    .filter(product -> product != null && product.getUser() != null)
                    .anyMatch(product -> product.getUser().getUserId().equals(sellerId));
            if (!ownsOrder) {
                throw new AccessDeniedException("You cannot update an order that has none of your products.");
            }
            if ("Cancelled".equals(normalizedStatus) || "Accepted".equals(normalizedStatus)) {
                throw new AccessDeniedException("Sellers may move orders to Processing, Shipped, or Delivered.");
            }
        }

        order.setOrderStatus(normalizedStatus);
        return modelMapper.map(orderRepository.save(order), OrderDTO.class);
    }

    @Override
    @Transactional(Transactional.TxType.SUPPORTS)
    public OrderResponse getAllSellerOrders(Integer pageNumber, Integer pageSize, String sortBy, String sortOrder) {
        validatePaging(pageNumber, pageSize);
        Set<String> allowedSorts = Set.of("orderId", "orderDate", "totalAmount", "orderStatus");
        String sortField = sortBy != null && allowedSorts.contains(sortBy) ? sortBy : "orderDate";
        Sort sort = "asc".equalsIgnoreCase(sortOrder) ? Sort.by(sortField).ascending() : Sort.by(sortField).descending();
        Pageable page = PageRequest.of(pageNumber, pageSize, sort);
        return toResponse(orderRepository.findSellerOrders(authUtil.loggedInUserId(), page));
    }

    private OrderResponse toResponse(Page<Order> page) {
        OrderResponse response = new OrderResponse();
        response.setContent(page.getContent().stream().map(order -> modelMapper.map(order, OrderDTO.class)).toList());
        response.setPageNumber(page.getNumber());
        response.setPageSize(page.getSize());
        response.setTotalElements(page.getTotalElements());
        response.setTotalPages(page.getTotalPages());
        response.setLastPage(page.isLast());
        return response;
    }

    private long toMinorUnits(double amount) {
        return BigDecimal.valueOf(amount).movePointRight(2).setScale(0, RoundingMode.HALF_UP).longValueExact();
    }

    private String normalizeStatus(String status) {
        if (status == null || status.isBlank()) {
            return "";
        }
        String value = status.trim().toLowerCase();
        return Character.toUpperCase(value.charAt(0)) + value.substring(1);
    }

    private boolean isAdmin() {
        return SecurityContextHolder.getContext().getAuthentication().getAuthorities().stream()
                .anyMatch(authority -> authority.getAuthority().equals("ROLE_ADMIN"));
    }

    private void validatePaging(Integer pageNumber, Integer pageSize) {
        if (pageNumber == null || pageNumber < 0 || pageSize == null || pageSize < 1 || pageSize > 100) {
            throw new APIException("pageNumber must be non-negative and pageSize must be between 1 and 100.");
        }
    }
}
