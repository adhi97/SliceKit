package io.slicekit.examples;

import io.slicekit.annotations.HttpMethod;
import io.slicekit.annotations.Slice;
import io.slicekit.annotations.SliceHandler;
import javax.inject.Inject;
import java.time.LocalDateTime;
import java.util.UUID;

/**
 * Example slice demonstrating dependency injection.
 * 
 * Shows how slice components are automatically injected into slice handlers.
 */
@Slice(
    name = "create-order", 
    route = "/api/orders", 
    method = HttpMethod.POST,
    description = "Creates a new order with dependency injection"
)
public final class CreateOrderSlice {
    
    private final OrderDataAccess orderDataAccess;
    private final OrderEmailService emailService;
    
    /**
     * Constructor with dependency injection.
     * SliceKit will automatically inject these dependencies.
     */
    @Inject
    public CreateOrderSlice(final OrderDataAccess orderDataAccess, final OrderEmailService emailService) {
        this.orderDataAccess = orderDataAccess;
        this.emailService = emailService;
    }
    
    @SliceHandler
    public CreateOrderResult handle(CreateOrderRequest request) {
        if (request == null) {
            throw new IllegalArgumentException("Order request cannot be null");
        }
        
        // Generate order ID
        final String orderId = UUID.randomUUID().toString();
        
        // Save order using injected data access
        final OrderData orderData = new OrderData(
            orderId,
            request.getCustomerId(),
            request.getItems(),
            request.getTotalAmount(),
            LocalDateTime.now()
        );
        
        orderDataAccess.saveOrder(orderData);
        
        // Send confirmation email using injected service
        emailService.sendOrderConfirmation(request.getCustomerId(), orderId);
        
        return new CreateOrderResult(
            orderId,
            "Order created successfully",
            orderData.getCreatedAt(),
            "Confirmation email sent"
        );
    }
    
    /**
     * Request object for create order.
     */
    public static final class CreateOrderRequest {
        private String customerId;
        private String[] items;
        private double totalAmount;
        
        // Default constructor for Jackson
        public CreateOrderRequest() {}
        
        public CreateOrderRequest(String customerId, String[] items, double totalAmount) {
            this.customerId = customerId;
            this.items = items;
            this.totalAmount = totalAmount;
        }
        
        public String getCustomerId() {
            return customerId;
        }
        
        public void setCustomerId(String customerId) {
            this.customerId = customerId;
        }
        
        public String[] getItems() {
            return items;
        }
        
        public void setItems(String[] items) {
            this.items = items;
        }
        
        public double getTotalAmount() {
            return totalAmount;
        }
        
        public void setTotalAmount(double totalAmount) {
            this.totalAmount = totalAmount;
        }
    }
    
    /**
     * Response object for create order.
     */
    public static final class CreateOrderResult {
        private final String orderId;
        private final String message;
        private final LocalDateTime createdAt;
        private final String emailStatus;
        
        public CreateOrderResult(String orderId, String message, LocalDateTime createdAt, String emailStatus) {
            this.orderId = orderId;
            this.message = message;
            this.createdAt = createdAt;
            this.emailStatus = emailStatus;
        }
        
        public String getOrderId() {
            return orderId;
        }
        
        public String getMessage() {
            return message;
        }
        
        public LocalDateTime getCreatedAt() {
            return createdAt;
        }
        
        public String getEmailStatus() {
            return emailStatus;
        }
        
        public String getSliceInfo() {
            return "Processed by CreateOrderSlice with dependency injection";
        }
    }
    
    /**
     * Internal order data structure.
     */
    public static final class OrderData {
        private final String orderId;
        private final String customerId;
        private final String[] items;
        private final double totalAmount;
        private final LocalDateTime createdAt;
        
        public OrderData(String orderId, String customerId, String[] items, double totalAmount, LocalDateTime createdAt) {
            this.orderId = orderId;
            this.customerId = customerId;
            this.items = items;
            this.totalAmount = totalAmount;
            this.createdAt = createdAt;
        }
        
        public String getOrderId() {
            return orderId;
        }
        
        public String getCustomerId() {
            return customerId;
        }
        
        public String[] getItems() {
            return items;
        }
        
        public double getTotalAmount() {
            return totalAmount;
        }
        
        public LocalDateTime getCreatedAt() {
            return createdAt;
        }
    }
}