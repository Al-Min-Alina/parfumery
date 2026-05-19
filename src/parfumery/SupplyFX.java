package parfumery;

import javafx.beans.property.*;

public class SupplyFX {
    private final IntegerProperty id;
    private final StringProperty date;
    private final StringProperty supplier;
    private final StringProperty receiver;
    private final DoubleProperty totalAmount;
    private final StringProperty items;

    public SupplyFX(int id, String date, String supplier, String receiver,
                    double totalAmount, String items) {
        this.id = new SimpleIntegerProperty(id);
        this.date = new SimpleStringProperty(date);
        this.supplier = new SimpleStringProperty(supplier);
        this.receiver = new SimpleStringProperty(receiver);
        this.totalAmount = new SimpleDoubleProperty(totalAmount);
        this.items = new SimpleStringProperty(items);
    }

    public IntegerProperty idProperty() { return id; }
    public StringProperty dateProperty() { return date; }
    public StringProperty supplierProperty() { return supplier; }
    public StringProperty receiverProperty() { return receiver; }
    public DoubleProperty totalAmountProperty() { return totalAmount; }
    public StringProperty itemsProperty() { return items; }

    public int getId() { return id.get(); }
    public double getTotalAmount() { return totalAmount.get(); }
    public String getDate() { return date.get(); }
    public String getSupplier() { return supplier.get(); }
    public String getReceiver() { return receiver.get(); }
    public String getItems() { return items.get(); }
}