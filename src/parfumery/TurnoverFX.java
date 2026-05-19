package parfumery;

import javafx.beans.property.*;

public class TurnoverFX {
    private final StringProperty name;
    private final IntegerProperty sold;
    private final IntegerProperty stock;
    private final DoubleProperty ratio;

    public TurnoverFX(String name, int sold, int stock, double ratio) {
        this.name = new SimpleStringProperty(name);
        this.sold = new SimpleIntegerProperty(sold);
        this.stock = new SimpleIntegerProperty(stock);
        this.ratio = new SimpleDoubleProperty(ratio);
    }

    public StringProperty nameProperty() { return name; }
    public IntegerProperty soldProperty() { return sold; }
    public IntegerProperty stockProperty() { return stock; }
    public DoubleProperty ratioProperty() { return ratio; }

    public String getName() { return name.get(); }
    public int getSold() { return sold.get(); }
    public int getStock() { return stock.get(); }
    public double getRatio() { return ratio.get(); }
}