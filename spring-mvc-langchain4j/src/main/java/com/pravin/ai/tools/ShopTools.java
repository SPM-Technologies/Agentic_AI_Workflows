package com.pravin.ai.tools;

import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Map;

import org.springframework.stereotype.Component;

import dev.langchain4j.agent.tool.P;
import dev.langchain4j.agent.tool.Tool;

/**
 * Tools = normal Java methods that Claude may decide to call.
 * The @Tool text tells Claude WHAT the method does, so it knows WHEN to use it.
 * Claude never runs code itself - it asks, LangChain4j calls the method,
 * and the return value goes back to Claude.
 */
@Component
public class ShopTools {

    // Pretend database - in a real app this would be a DAO / repository call
    private static final Map<String, String> ORDERS = Map.of(
            "101", "Shipped on 20-10-2026, expected delivery 24-10-2026",
            "102", "Packed, waiting for courier pickup",
            "103", "Cancelled by customer on 18-10-2026, refund issued");

    @Tool("Returns the current date and time in India (IST)")
    public String currentDateTime() {
        return ZonedDateTime.now(ZoneId.of("Asia/Kolkata"))
                .format(DateTimeFormatter.ofPattern("EEEE, dd-MM-yyyy HH:mm"));
    }

    @Tool("Returns the status of a customer order")
    public String getOrderStatus(@P("Order id, for example 101") String orderId) {
        return ORDERS.getOrDefault(orderId.trim(), "No order found with id " + orderId);
    }
}
