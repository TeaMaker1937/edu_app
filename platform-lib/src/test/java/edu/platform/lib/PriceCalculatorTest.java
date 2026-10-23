package edu.platform.lib;

import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class PriceCalculatorTest {

    @Test
    void discount0() {
        assertEquals(5000.0, PriceCalculator.calculateDiscountedPrice(5000, 0), 1e-9);
    }

    @Test
    void discount10() {
        assertEquals(4500.0, PriceCalculator.calculateDiscountedPrice(5000, 10), 1e-9);
    }

    @Test
    void discount100() {
        assertEquals(0.0, PriceCalculator.calculateDiscountedPrice(5000, 100), 1e-9);
    }

    @Test
    void discountNegative() {
        assertThrows(IllegalArgumentException.class,
                () -> PriceCalculator.calculateDiscountedPrice(5000, -1));
    }

    @Test
    void discountOver100() {
        assertThrows(IllegalArgumentException.class,
                () -> PriceCalculator.calculateDiscountedPrice(5000, 101));
    }
}