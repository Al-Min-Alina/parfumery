package parfumery;

import java.math.BigDecimal;

import java.io.Serializable;

public class Product implements Serializable, Reportable {
    private static final long serialVersionUID = 1L;

    private int id;
    private String name;
    private int categoryId;
    private String categoryName;
    private String brand;
    private int volumeMl;
    private BigDecimal purchasePrice;
    private BigDecimal retailPrice;
    private int stockQuantity;
    private String description;

    public Product(int id, String name, int categoryId, String brand,
                   int volumeMl, BigDecimal purchasePrice, BigDecimal retailPrice,
                   int stockQuantity, String description) {
        this.id = id;
        this.name = name;
        this.categoryId = categoryId;
        this.brand = brand;
        this.volumeMl = volumeMl;
        this.purchasePrice = purchasePrice;
        this.retailPrice = retailPrice;
        this.stockQuantity = stockQuantity;
        this.description = description;
    }

    public int getId() { return id; }
    public String getName() { return name; }
    public int getCategoryId() { return categoryId; }
    public String getCategoryName() { return categoryName; }
    public void setCategoryName(String categoryName) { this.categoryName = categoryName; }
    public String getBrand() { return brand; }
    public int getVolumeMl() { return volumeMl; }
    public BigDecimal getPurchasePrice() { return purchasePrice; }
    public BigDecimal getRetailPrice() { return retailPrice; }
    public int getStockQuantity() { return stockQuantity; }
    public String getDescription() { return description; }

    @Override
    public String toString() {
        return String.format("[%d] %s (%s, %d мл) | Категория: %s | Цена: %.2f руб. | Остаток: %d шт.",
                id, name, brand != null ? brand : "без бренда", volumeMl,
                categoryName != null ? categoryName : "-", retailPrice, stockQuantity);
    }

    @Override
    public String toReportString() {
        return String.format("Товар: %s | Бренд: %s | Объём: %d мл | Цена: %.2f руб. | Остаток: %d шт.",
                name, brand != null ? brand : "-", volumeMl, retailPrice, stockQuantity);
    }
}