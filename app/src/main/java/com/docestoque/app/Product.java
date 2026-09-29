package com.docestoque.app;

public class Product {
    private long id;
    private String name;
    private String description;
    private String location;
    private int quantity;
    private double costPrice;
    private double salePrice;
    private String imagePath;
    private boolean active = true;
    private int minimumStock;

    public Product() {
    }

    public Product(long id, String name, String description, String location, int quantity,
                   double costPrice, double salePrice, String imagePath) {
        this.id = id;
        this.name = name;
        this.description = description;
        this.location = location;
        this.quantity = quantity;
        this.costPrice = costPrice;
        this.salePrice = salePrice;
        this.imagePath = imagePath;
    }

    public long getId() {
        return id;
    }

    public void setId(long id) {
        this.id = id;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public String getLocation() {
        return location;
    }

    public void setLocation(String location) {
        this.location = location;
    }

    public int getQuantity() {
        return quantity;
    }

    public void setQuantity(int quantity) {
        this.quantity = quantity;
    }

    public double getCostPrice() {
        return costPrice;
    }

    public void setCostPrice(double costPrice) {
        this.costPrice = costPrice;
    }

    public double getSalePrice() {
        return salePrice;
    }

    public void setSalePrice(double salePrice) {
        this.salePrice = salePrice;
    }

    public String getImagePath() {
        return imagePath;
    }

    public void setImagePath(String imagePath) {
        this.imagePath = imagePath;
    }

    public boolean isActive() {
        return active;
    }

    public void setActive(boolean active) {
        this.active = active;
    }

    public int getMinimumStock() {
        return minimumStock;
    }

    public void setMinimumStock(int minimumStock) {
        this.minimumStock = minimumStock;
    }
}
