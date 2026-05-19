package parfumery;

/**
 * Модель пользователя системы.
 */
public class User {
    private int id;
    private String login;
    private String fullName;
    private String roleName;

    public User(int id, String login, String fullName, String roleName) {
        this.id = id;
        this.login = login;
        this.fullName = fullName;
        this.roleName = roleName;
    }

    public int getId() { return id; }
    public String getLogin() { return login; }
    public String getFullName() { return fullName; }
    public String getRoleName() { return roleName; }

    public boolean isAdmin() { return "ADMIN".equalsIgnoreCase(roleName); }
    public boolean isManager() { return "MANAGER".equalsIgnoreCase(roleName); }
    public boolean isAnalyst() { return "ANALYST".equalsIgnoreCase(roleName); }

    @Override
    public String toString() {
        return fullName + " (" + roleName + ")";
    }
}