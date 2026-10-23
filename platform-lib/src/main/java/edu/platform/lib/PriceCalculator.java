package edu.platform.lib;

public final class PriceCalculator {

    private PriceCalculator() {}

    public static double calculateDiscountedPrice(double price, double discount) {
        if (price < 0) {
            throw new IllegalArgumentException("Price must be >= 0");
        }
        if (discount < 0 || discount > 100) {
            throw new IllegalArgumentException("Discount must be between 0 and 100");
        }
        return price * (1.0 - discount / 100.0);
    }
}