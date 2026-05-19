package parfumery;

import javafx.beans.property.*;

public class SaleFX {
    private final IntegerProperty id;
    private final StringProperty date;
    private final StringProperty client;
    private final StringProperty seller;
    private final DoubleProperty totalAmount;
    private final StringProperty items;

    public SaleFX(int id, String date, String client, String seller,
                  double totalAmount, String items) {
        this.id = new SimpleIntegerProperty(id);
        this.date = new SimpleStringProperty(date);
        this.client = new SimpleStringProperty(client);
        this.seller = new SimpleStringProperty(seller);
        this.totalAmount = new SimpleDoubleProperty(totalAmount);
        this.items = new SimpleStringProperty(items);
    }

    public IntegerProperty idProperty() { return id; }
    public StringProperty dateProperty() { return date; }
    public StringProperty clientProperty() { return client; }
    public StringProperty sellerProperty() { return seller; }
    public DoubleProperty totalAmountProperty() { return totalAmount; }
    public StringProperty itemsProperty() { return items; }

    // GETTER-ы для поиска
    public int getId() { return id.get(); }
    public String getDate() { return date.get(); }
    public String getClient() { return client.get(); }
    public String getSeller() { return seller.get(); }
    public String getItems() { return items.get(); }

    public double getTotalAmount() { return totalAmount.get(); }
}