package com.docestoque.app;

public class Movement {
    private final String productName;
    private final String type;
    private final int quantity;
    private final long createdAt;

    public Movement(String productName, String type, int quantity, long createdAt) {
        this.productName = productName;
        this.type = type;
        this.quantity = quantity;
        this.createdAt = createdAt;
    }

    public String getProductName() {
        return productName;
    }

    public String getType() {
        return type;
    }

    public int getQuantity() {
        return quantity;
    }

    public long getCreatedAt() {
        return createdAt;
    }
}