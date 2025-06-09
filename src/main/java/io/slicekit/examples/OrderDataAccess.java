package io.slicekit.examples;

import io.slicekit.injection.SliceComponent;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.concurrent.ConcurrentHashMap;
import java.util.Map;

/**
 * Slice component for order data access.
 * 
 * This is specific to the create-order slice and demonstrates
 * slice-scoped dependency injection.
 */
@SliceComponent("create-order")
public final class OrderDataAccess {
    
    private static final Logger logger = LoggerFactory.getLogger(OrderDataAccess.class);
    
    // In-memory storage for demo purposes
    private final Map<String, CreateOrderSlice.OrderData> orders = new ConcurrentHashMap<>();
    
    /**
     * Saves an order to storage.
     */
    public void saveOrder(final CreateOrderSlice.OrderData orderData) {
        logger.info("Saving order: {} for customer: {}", 
            orderData.getOrderId(), orderData.getCustomerId());
        
        orders.put(orderData.getOrderId(), orderData);
        
        logger.debug("Order saved successfully. Total orders in storage: {}", orders.size());
    }
    
    /**
     * Gets an order by ID.
     */
    public CreateOrderSlice.OrderData getOrder(final String orderId) {
        final CreateOrderSlice.OrderData order = orders.get(orderId);
        
        if (order != null) {
            logger.debug("Retrieved order: {} for customer: {}", 
                order.getOrderId(), order.getCustomerId());
        } else {
            logger.warn("Order not found: {}", orderId);
        }
        
        return order;
    }
    
    /**
     * Gets total number of orders (for demo purposes).
     */
    public int getTotalOrderCount() {
        return orders.size();
    }
    
    /**
     * Checks if an order exists.
     */
    public boolean orderExists(final String orderId) {
        return orders.containsKey(orderId);
    }
}