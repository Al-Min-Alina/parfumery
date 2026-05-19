package parfumery;

import javafx.beans.property.*;

public class SupplierFX {
    private final IntegerProperty id;
    private final StringProperty name;
    private final StringProperty contactPerson;
    private final StringProperty phone;
    private final StringProperty email;
    private final StringProperty address;

    public SupplierFX(int id, String name, String contactPerson, String phone, String email, String address) {
        this.id = new SimpleIntegerProperty(id);
        this.name = new SimpleStringProperty(name);
        this.contactPerson = new SimpleStringProperty(contactPerson);
        this.phone = new SimpleStringProperty(phone);
        this.email = new SimpleStringProperty(email);
        this.address = new SimpleStringProperty(address);
    }

    public IntegerProperty idProperty() { return id; }
    public StringProperty nameProperty() { return name; }
    public StringProperty contactPersonProperty() { return contactPerson; }
    public StringProperty phoneProperty() { return phone; }
    public StringProperty emailProperty() { return email; }
    public StringProperty addressProperty() { return address; }

    public int getId() { return id.get(); }
    public String getName() { return name.get(); }
    public String getContactPerson() { return contactPerson.get(); }
    public String getPhone() { return phone.get(); }
    public String getEmail() { return email.get(); }
    public String getAddress() { return address.get(); }
}