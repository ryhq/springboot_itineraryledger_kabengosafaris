package com.itineraryledger.kabengosafaris.Quote.Embeddables;

import java.math.BigDecimal;

import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Embeddable class representing a price in a specific currency.
 * Used for multi-currency support in Quote and QuoteItem entities.
 *
 * A single quote item can have multiple prices (one per currency) to accommodate
 * passengers with different nationality categories (e.g., Non-Resident pays USD, Tanzanian pays TZS).
 */
@Embeddable
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Price {

    /**
     * Currency code (ISO 4217 format)
     * Examples: USD, TZS, EUR, GBP
     */
    @Column(nullable = false, length = 3)
    private String currency;

    /**
     * Quantity of items/units at this price
     * Example: 2 rooms, 3 passengers, 1 vehicle
     */
    @Column(nullable = false)
    private Integer quantity;

    /**
     * Price per unit in the specified currency
     */
    @Column(nullable = false, precision = 19, scale = 2)
    private BigDecimal unitPrice;

    /**
     * Total price (quantity × unitPrice) in the specified currency
     * This is calculated but stored for consistency and query performance
     */
    @Column(nullable = false, precision = 19, scale = 2)
    private BigDecimal totalPrice;

    /**
     * Optional breakdown or explanation of how this price was calculated
     * Example: "2 Adults × $50 + 1 Child × $25"
     */
    @Column(length = 500)
    private String breakdown;
}
