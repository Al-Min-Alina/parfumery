package parfumery;

import javafx.beans.property.*;

import java.io.Serializable;

public class ProductFX implements Serializable, Reportable {
    private static final long serialVersionUID = 1L;

    private final IntegerProperty id;
    private final StringProperty name;
    private final StringProperty brand;
    private final StringProperty categoryName;
    private final IntegerProperty volumeMl;
    private final DoubleProperty purchasePrice;
    private final DoubleProperty retailPrice;
    private final IntegerProperty stockQuantity;
    private final StringProperty description;

    public ProductFX(int id, String name, String brand, String categoryName,
                     int volumeMl, double purchasePrice, double retailPrice,
                     int stockQuantity, String description) {
        this.id = new SimpleIntegerProperty(id);
        this.name = new SimpleStringProperty(name);
        this.brand = new SimpleStringProperty(brand);
        this.categoryName = new SimpleStringProperty(categoryName);
        this.volumeMl = new SimpleIntegerProperty(volumeMl);
        this.purchasePrice = new SimpleDoubleProperty(purchasePrice);
        this.retailPrice = new SimpleDoubleProperty(retailPrice);
        this.stockQuantity = new SimpleIntegerProperty(stockQuantity);
        this.description = new SimpleStringProperty(description);
    }

    public IntegerProperty idProperty() { return id; }
    public StringProperty nameProperty() { return name; }
    public StringProperty brandProperty() { return brand; }
    public StringProperty categoryNameProperty() { return categoryName; }
    public IntegerProperty volumeMlProperty() { return volumeMl; }
    public DoubleProperty purchasePriceProperty() { return purchasePrice; }
    public DoubleProperty retailPriceProperty() { return retailPrice; }
    public IntegerProperty stockQuantityProperty() { return stockQuantity; }
    public StringProperty descriptionProperty() { return description; }

    public int getId() { return id.get(); }
    public String getName() { return name.get(); }
    public String getBrand() { return brand.get(); }
    public String getCategoryName() { return categoryName.get(); }
    public int getVolumeMl() { return volumeMl.get(); }
    public double getPurchasePrice() { return purchasePrice.get(); }
    public double getRetailPrice() { return retailPrice.get(); }
    public int getStockQuantity() { return stockQuantity.get(); }
    public String getDescription() { return description.get(); }

    @Override
    public String toReportString() {
        return String.format("Товар: %s | Бренд: %s | Объём: %d мл | Цена: %.2f руб. | Остаток: %d шт.",
                getName(), getBrand(), getVolumeMl(), getRetailPrice(), getStockQuantity());
    }
}

