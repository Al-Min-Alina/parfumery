package parfumery;

import javafx.beans.property.*;

public class AbcFX {
    private final StringProperty group;
    private final StringProperty name;
    private final StringProperty revenue;
    private final StringProperty share;
    private final StringProperty cumulative;

    public AbcFX(String group, String name, String revenue, String share, String cumulative) {
        this.group = new SimpleStringProperty(group);
        this.name = new SimpleStringProperty(name);
        this.revenue = new SimpleStringProperty(revenue);
        this.share = new SimpleStringProperty(share);
        this.cumulative = new SimpleStringProperty(cumulative);
    }

    public StringProperty groupProperty() { return group; }
    public StringProperty nameProperty() { return name; }
    public StringProperty revenueProperty() { return revenue; }
    public StringProperty shareProperty() { return share; }
    public StringProperty cumulativeProperty() { return cumulative; }

    public String getGroup() { return group.get(); }
    public String getName() { return name.get(); }
    public String getRevenue() { return revenue.get(); }
    public String getShare() { return share.get(); }
    public String getCumulative() { return cumulative.get(); }
}