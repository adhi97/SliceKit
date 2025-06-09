package io.slicekit.examples;

import io.slicekit.injection.SliceComponent;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Slice component for order email notifications.
 * 
 * This is specific to the create-order slice and demonstrates
 * slice-scoped dependency injection.
 */
@SliceComponent("create-order")
public final class OrderEmailService {
    
    private static final Logger logger = LoggerFactory.getLogger(OrderEmailService.class);
    
    /**
     * Sends order confirmation email to customer.
     */
    public void sendOrderConfirmation(final String customerId, final String orderId) {
        logger.info("Sending order confirmation email for order: {} to customer: {}", 
            orderId, customerId);
        
        // Simulate email sending
        try {
            Thread.sleep(100); // Simulate email processing time
            logger.info("Order confirmation email sent successfully for order: {}", orderId);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            logger.error("Failed to send order confirmation email for order: {}", orderId, e);
        }
    }
    
    /**
     * Sends order cancellation email to customer.
     */
    public void sendOrderCancellation(final String customerId, final String orderId) {
        logger.info("Sending order cancellation email for order: {} to customer: {}", 
            orderId, customerId);
        
        // Simulate email sending
        try {
            Thread.sleep(100); // Simulate email processing time
            logger.info("Order cancellation email sent successfully for order: {}", orderId);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            logger.error("Failed to send order cancellation email for order: {}", orderId, e);
        }
    }
    
    /**
     * Gets email service status (for demo purposes).
     */
    public String getServiceStatus() {
        return "OrderEmailService is running and ready to send emails";
    }
}