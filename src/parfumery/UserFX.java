package parfumery;

import javafx.beans.property.*;

public class UserFX {
    private final IntegerProperty id;
    private final StringProperty login;
    private final StringProperty fullName;
    private final StringProperty role;
    private final StringProperty status;

    public UserFX(int id, String login, String fullName, String role, String status) {
        this.id = new SimpleIntegerProperty(id);
        this.login = new SimpleStringProperty(login);
        this.fullName = new SimpleStringProperty(fullName);
        this.role = new SimpleStringProperty(role);
        this.status = new SimpleStringProperty(status);
    }

    public IntegerProperty idProperty() { return id; }
    public StringProperty loginProperty() { return login; }
    public StringProperty fullNameProperty() { return fullName; }
    public StringProperty roleProperty() { return role; }
    public StringProperty statusProperty() { return status; }

    public int getId() { return id.get(); }
    public String getLogin() { return login.get(); }
    public String getFullName() { return fullName.get(); }
    public String getRole() { return role.get(); }
    public String getStatus() { return status.get(); }
}