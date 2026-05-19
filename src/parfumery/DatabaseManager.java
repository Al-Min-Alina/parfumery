package parfumery;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;

public class DatabaseManager {

    private Connection connection;

    public void connect() throws SQLException {
        try {
            Class.forName("com.mysql.cj.jdbc.Driver");
        } catch (ClassNotFoundException e) {
            throw new SQLException("Драйвер MySQL не найден. Проверь JAR в папке lib!", e);
        }

        String url = DatabaseConfig.getConnectionUrl();
        System.out.println("Подключаюсь к БД: " + url);

        connection = DriverManager.getConnection(
                url,
                DatabaseConfig.getDbUser(),
                DatabaseConfig.getDbPassword()
        );

        try (Statement stmt = connection.createStatement()) {
            stmt.execute("SET NAMES utf8mb4");
            stmt.execute("SET CHARACTER SET utf8mb4");
            stmt.execute("SET character_set_connection=utf8mb4");
        }

        System.out.println("Подключение к MySQL установлено успешно.");
        System.out.println("Кодировка соединения: utf8mb4");
    }

    public List<Product> getAllProducts() throws SQLException {
        List<Product> products = new ArrayList<>();

        String sql = "SELECT p.id, p.name, p.category_id, c.name AS category_name, " +
                "p.brand, p.volume_ml, p.purchase_price, p.retail_price, " +
                "p.stock_quantity, p.description " +
                "FROM products p " +
                "JOIN categories c ON p.category_id = c.id " +
                "WHERE p.is_active = TRUE " +
                "ORDER BY p.name";

        try (Statement stmt = connection.createStatement();
             ResultSet rs = stmt.executeQuery(sql)) {

            while (rs.next()) {
                Product product = new Product(
                        rs.getInt("id"),
                        rs.getString("name"),
                        rs.getInt("category_id"),
                        rs.getString("brand"),
                        rs.getInt("volume_ml"),
                        rs.getBigDecimal("purchase_price"),
                        rs.getBigDecimal("retail_price"),
                        rs.getInt("stock_quantity"),
                        rs.getString("description")
                );
                product.setCategoryName(rs.getString("category_name"));
                products.add(product);
            }
        }

        return products;
    }

    public List<Product> getProductsByCategory(int categoryId) throws SQLException {
        List<Product> products = new ArrayList<>();

        String sql = "SELECT p.id, p.name, p.category_id, c.name AS category_name, " +
                "p.brand, p.volume_ml, p.purchase_price, p.retail_price, " +
                "p.stock_quantity, p.description " +
                "FROM products p " +
                "JOIN categories c ON p.category_id = c.id " +
                "WHERE p.category_id = ? AND p.is_active = TRUE " +
                "ORDER BY p.name";

        try (PreparedStatement pstmt = connection.prepareStatement(sql)) {
            pstmt.setInt(1, categoryId);
            try (ResultSet rs = pstmt.executeQuery()) {
                while (rs.next()) {
                    Product product = new Product(
                            rs.getInt("id"),
                            rs.getString("name"),
                            rs.getInt("category_id"),
                            rs.getString("brand"),
                            rs.getInt("volume_ml"),
                            rs.getBigDecimal("purchase_price"),
                            rs.getBigDecimal("retail_price"),
                            rs.getInt("stock_quantity"),
                            rs.getString("description")
                    );
                    product.setCategoryName(rs.getString("category_name"));
                    products.add(product);
                }
            }
        }
        return products;
    }

    public void disconnect() {
        if (connection != null) {
            try {
                connection.close();
                System.out.println("Соединение с MySQL закрыто.");
            } catch (SQLException e) {
                System.err.println("Ошибка при закрытии БД: " + e.getMessage());
            }
        }
    }

    public User authenticate(String login, String password) throws SQLException {
        String sql = "SELECT u.id, u.login, u.full_name, r.name AS role_name " +
                "FROM users u " +
                "JOIN roles r ON u.role_id = r.id " +
                "WHERE u.login = ? AND u.password = ? AND u.is_active = TRUE";

        try (PreparedStatement pstmt = connection.prepareStatement(sql)) {
            pstmt.setString(1, login);
            pstmt.setString(2, password);

            try (ResultSet rs = pstmt.executeQuery()) {
                if (rs.next()) {
                    return new User(
                            rs.getInt("id"),
                            rs.getString("login"),
                            rs.getString("full_name"),
                            rs.getString("role_name")
                    );
                }
            }
        }
        return null;
    }

    public List<User> getAllUsers() throws SQLException {
        List<User> users = new ArrayList<>();

        String sql = "SELECT u.id, u.login, u.full_name, r.name AS role_name " +
                "FROM users u " +
                "JOIN roles r ON u.role_id = r.id " +
                "ORDER BY u.full_name";

        try (Statement stmt = connection.createStatement();
             ResultSet rs = stmt.executeQuery(sql)) {

            while (rs.next()) {
                users.add(new User(
                        rs.getInt("id"),
                        rs.getString("login"),
                        rs.getString("full_name"),
                        rs.getString("role_name")
                ));
            }
        }

        return users;
    }

    public int createSale(int userId, Integer clientId, List<String[]> items) throws SQLException {
        connection.setAutoCommit(false);
        try {
            double total = 0;
            for (String[] item : items) {
                total += Integer.parseInt(item[1]) * Double.parseDouble(item[2]);
            }

            String saleSql;
            if (clientId != null) {
                saleSql = "INSERT INTO sales (client_id, user_id, total_amount) VALUES (?, ?, ?)";
            } else {
                saleSql = "INSERT INTO sales (user_id, total_amount) VALUES (?, ?)";
            }

            int saleId;
            try (PreparedStatement pstmt = connection.prepareStatement(saleSql,
                    Statement.RETURN_GENERATED_KEYS)) {
                if (clientId != null) {
                    pstmt.setInt(1, clientId);
                    pstmt.setInt(2, userId);
                    pstmt.setDouble(3, total);
                } else {
                    pstmt.setInt(1, userId);
                    pstmt.setDouble(2, total);
                }
                pstmt.executeUpdate();

                try (ResultSet rs = pstmt.getGeneratedKeys()) {
                    rs.next();
                    saleId = rs.getInt(1);
                }
            }

            String itemSql = "INSERT INTO sales_items (sale_id, product_id, quantity, price) VALUES (?, ?, ?, ?)";
            String updateStockSql = "UPDATE products SET stock_quantity = stock_quantity - ? WHERE id = ? AND stock_quantity >= ?";

            try (PreparedStatement itemStmt = connection.prepareStatement(itemSql);
                 PreparedStatement stockStmt = connection.prepareStatement(updateStockSql)) {

                for (String[] item : items) {
                    int productId = Integer.parseInt(item[0]);
                    int quantity = Integer.parseInt(item[1]);
                    double price = Double.parseDouble(item[2]);

                    stockStmt.setInt(1, quantity);
                    stockStmt.setInt(2, productId);
                    stockStmt.setInt(3, quantity);
                    int updated = stockStmt.executeUpdate();
                    if (updated == 0) {
                        throw new SQLException("Недостаточно товара с ID=" + productId + " на складе!");
                    }

                    itemStmt.setInt(1, saleId);
                    itemStmt.setInt(2, productId);
                    itemStmt.setInt(3, quantity);
                    itemStmt.setDouble(4, price);
                    itemStmt.executeUpdate();
                }
            }

            connection.commit();
            return saleId;
        } catch (SQLException e) {
            connection.rollback();
            throw e;
        } finally {
            connection.setAutoCommit(true);
        }
    }

    public int createSupply(int supplierId, String supplierName, int userId, List<String[]> items) throws SQLException {
        connection.setAutoCommit(false);
        try {
            double total = 0;
            for (String[] item : items) {
                total += Integer.parseInt(item[1]) * Double.parseDouble(item[2].replace(",", "."));
            }

            String supplySql;
            if (supplierId > 0) {
                supplySql = "INSERT INTO supplies (supplier_id, supplier_name, user_id, total_amount) VALUES (?, ?, ?, ?)";
            } else {
                int newId = addSupplierAndGetId(supplierName);
                supplySql = "INSERT INTO supplies (supplier_id, supplier_name, user_id, total_amount) VALUES (?, ?, ?, ?)";
                supplierId = newId;
            }

            int supplyId;
            try (PreparedStatement pstmt = connection.prepareStatement(supplySql, Statement.RETURN_GENERATED_KEYS)) {
                pstmt.setInt(1, supplierId);
                pstmt.setString(2, supplierName);
                pstmt.setInt(3, userId);
                pstmt.setDouble(4, total);
                pstmt.executeUpdate();

                try (ResultSet rs = pstmt.getGeneratedKeys()) {
                    rs.next();
                    supplyId = rs.getInt(1);
                }
            }

            String itemSql = "INSERT INTO supplies_items (supply_id, product_id, quantity, purchase_price) VALUES (?, ?, ?, ?)";
            String updateStockSql = "UPDATE products SET stock_quantity = stock_quantity + ? WHERE id = ?";

            try (PreparedStatement itemStmt = connection.prepareStatement(itemSql);
                 PreparedStatement stockStmt = connection.prepareStatement(updateStockSql)) {

                for (String[] item : items) {
                    int productId = Integer.parseInt(item[0]);
                    int quantity = Integer.parseInt(item[1]);
                    double price = Double.parseDouble(item[2].replace(",", "."));

                    stockStmt.setInt(1, quantity);
                    stockStmt.setInt(2, productId);
                    stockStmt.executeUpdate();

                    itemStmt.setInt(1, supplyId);
                    itemStmt.setInt(2, productId);
                    itemStmt.setInt(3, quantity);
                    itemStmt.setDouble(4, price);
                    itemStmt.executeUpdate();
                }
            }

            connection.commit();
            return supplyId;
        } catch (SQLException e) {
            connection.rollback();
            throw e;
        } finally {
            connection.setAutoCommit(true);
        }
    }

    private int addSupplierAndGetId(String name) throws SQLException {
        String sql = "INSERT INTO suppliers (name) VALUES (?)";
        try (PreparedStatement pstmt = connection.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            pstmt.setString(1, name);
            pstmt.executeUpdate();
            try (ResultSet rs = pstmt.getGeneratedKeys()) {
                rs.next();
                return rs.getInt(1);
            }
        }
    }

    public void addSupplier(String name, String contactPerson, String phone, String email, String address) throws SQLException {
        String sql = "INSERT INTO suppliers (name, contact_person, phone, email, address) VALUES (?, ?, ?, ?, ?)";
        try (PreparedStatement pstmt = connection.prepareStatement(sql)) {
            pstmt.setString(1, name);
            pstmt.setString(2, contactPerson);
            pstmt.setString(3, phone);
            pstmt.setString(4, email);
            pstmt.setString(5, address);
            pstmt.executeUpdate();
        }
    }

    public void deactivateSupplier(int id) throws SQLException {
        String sql = "UPDATE suppliers SET is_active = FALSE WHERE id=?";
        try (PreparedStatement ps = connection.prepareStatement(sql)) {
            ps.setInt(1, id);
            ps.executeUpdate();
        }
    }

    public void addClient(String name, String phone, String email, String address) throws SQLException {
        String sql = "INSERT INTO clients (name, phone, email, address) VALUES (?,?,?,?)";
        try (PreparedStatement ps = connection.prepareStatement(sql)) {
            ps.setString(1, name); ps.setString(2, phone);
            ps.setString(3, email); ps.setString(4, address);
            ps.executeUpdate();
        }
    }
    public void updateClient(int id, String name, String phone, String email, String address) throws SQLException {
        String sql = "UPDATE clients SET name=?, phone=?, email=?, address=? WHERE id=?";
        try (PreparedStatement ps = connection.prepareStatement(sql)) {
            ps.setString(1, name); ps.setString(2, phone);
            ps.setString(3, email); ps.setString(4, address);
            ps.setInt(5, id); ps.executeUpdate();
        }
    }
    public void deactivateClient(int id) throws SQLException {
        String sql = "UPDATE clients SET is_active=FALSE WHERE id=?";
        try (PreparedStatement ps = connection.prepareStatement(sql)) {
            ps.setInt(1, id); ps.executeUpdate();
        }
    }

    public List<String> getSalesHistory() throws SQLException {
        List<String> history = new ArrayList<>();
        String sql = "SELECT s.id, s.sale_date, COALESCE(c.name, 'Без клиента') AS client, " +
                "u.full_name AS seller, s.total_amount, " +
                "GROUP_CONCAT(CONCAT(p.name, ' x', si.quantity) SEPARATOR ', ') AS items " +
                "FROM sales s " +
                "JOIN users u ON s.user_id = u.id " +
                "LEFT JOIN clients c ON s.client_id = c.id " +
                "JOIN sales_items si ON s.id = si.sale_id " +
                "JOIN products p ON si.product_id = p.id " +
                "GROUP BY s.id " +
                "ORDER BY s.sale_date DESC";

        try (Statement stmt = connection.createStatement();
             ResultSet rs = stmt.executeQuery(sql)) {
            while (rs.next()) {
                history.add(String.format("[Продажа #%d] %s | Клиент: %s | Продавец: %s | Сумма: %.2f руб. | Товары: %s",
                        rs.getInt("id"),
                        rs.getTimestamp("sale_date").toString().substring(0, 16),
                        rs.getString("client"),
                        rs.getString("seller"),
                        rs.getDouble("total_amount"),
                        rs.getString("items")
                ));
            }
        }
        return history;
    }

    public List<String> getSuppliesHistory() throws SQLException {
        List<String> history = new ArrayList<>();
        String sql = "SELECT s.id, s.supply_date, " +
                "COALESCE(sp.name, s.supplier_name) AS supplier_name, " +
                "u.full_name AS receiver, s.total_amount, " +
                "GROUP_CONCAT(CONCAT(p.name, ' x', si.quantity) SEPARATOR ', ') AS items " +
                "FROM supplies s " +
                "JOIN users u ON s.user_id = u.id " +
                "LEFT JOIN suppliers sp ON s.supplier_id = sp.id " +
                "JOIN supplies_items si ON s.id = si.supply_id " +
                "JOIN products p ON si.product_id = p.id " +
                "GROUP BY s.id " +
                "ORDER BY s.supply_date DESC";

        try (Statement stmt = connection.createStatement();
             ResultSet rs = stmt.executeQuery(sql)) {
            while (rs.next()) {
                history.add(String.format("[Поставка #%d] %s | Поставщик: %s | Принял: %s | Сумма: %.2f руб. | Товары: %s",
                        rs.getInt("id"),
                        rs.getTimestamp("supply_date").toString().substring(0, 16),
                        rs.getString("supplier_name"),
                        rs.getString("receiver"),
                        rs.getDouble("total_amount"),
                        rs.getString("items")
                ));
            }
        }
        return history;
    }

    public List<String[]> getTurnover(int days) throws SQLException {
        List<String[]> result = new ArrayList<>();

        String sql = "SELECT p.id, p.name, " +
                "COALESCE(SUM(CASE WHEN s.sale_date >= DATE_SUB(NOW(), INTERVAL ? DAY) THEN si.quantity ELSE 0 END), 0) AS gross_sold, " +
                "COALESCE((SELECT SUM(r.quantity) FROM returns r " +
                "          JOIN sales s2 ON r.sale_id = s2.id " +
                "          WHERE r.product_id = p.id AND s2.sale_date >= DATE_SUB(NOW(), INTERVAL ? DAY)), 0) AS returned, " +
                "p.stock_quantity AS current_stock " +
                "FROM products p " +
                "LEFT JOIN sales_items si ON p.id = si.product_id " +
                "LEFT JOIN sales s ON si.sale_id = s.id " +
                "WHERE p.is_active = TRUE " +
                "GROUP BY p.id, p.name, p.stock_quantity " +
                "ORDER BY current_stock DESC";

        try (PreparedStatement pstmt = connection.prepareStatement(sql)) {
            pstmt.setInt(1, days);
            pstmt.setInt(2, days);
            try (ResultSet rs = pstmt.executeQuery()) {
                while (rs.next()) {
                    int grossSold = rs.getInt("gross_sold");
                    int returned = rs.getInt("returned");
                    int netSold = Math.max(0, grossSold - returned);
                    int stock = rs.getInt("current_stock");
                    double ratio = stock > 0 ? Math.round((double) netSold / stock * 100.0) / 100.0 : 0;

                    result.add(new String[]{
                            rs.getString("name"),
                            String.valueOf(netSold),
                            String.valueOf(stock),
                            String.format("%.2f", ratio)
                    });
                }
            }
        }
        return result;
    }

    public List<String[]> getAbcAnalysis(int days) throws SQLException {
        List<String[]> result = new ArrayList<>();

        String sql = "SELECT p.name, " +
                "COALESCE(SUM(CASE WHEN s.sale_date >= DATE_SUB(NOW(), INTERVAL ? DAY) THEN si.quantity * si.price ELSE 0 END), 0) AS gross_revenue, " +
                "COALESCE((SELECT SUM(r.quantity * si2.price) FROM returns r " +
                "          JOIN sales s2 ON r.sale_id = s2.id " +
                "          JOIN sales_items si2 ON r.sale_id = si2.sale_id AND r.product_id = si2.product_id " +
                "          WHERE r.product_id = p.id AND s2.sale_date >= DATE_SUB(NOW(), INTERVAL ? DAY)), 0) AS returned_revenue " +
                "FROM products p " +
                "LEFT JOIN sales_items si ON p.id = si.product_id " +
                "LEFT JOIN sales s ON si.sale_id = s.id " +
                "WHERE p.is_active = TRUE " +
                "GROUP BY p.id, p.name " +
                "ORDER BY gross_revenue DESC";

        double totalRevenue = 0;
        List<String[]> tempData = new ArrayList<>();

        try (PreparedStatement pstmt = connection.prepareStatement(sql)) {
            pstmt.setInt(1, days);
            pstmt.setInt(2, days);
            try (ResultSet rs = pstmt.executeQuery()) {
                while (rs.next()) {
                    double gross = rs.getDouble("gross_revenue");
                    double returned = rs.getDouble("returned_revenue");
                    double revenue = Math.max(0, gross - returned);
                    totalRevenue += revenue;
                    tempData.add(new String[]{
                            rs.getString("name"),
                            String.format("%.2f", revenue).replace(".", ",")
                    });
                }
            }
        }

        if (totalRevenue == 0 || tempData.isEmpty()) return result;

        double cumulativeShare = 0;
        for (String[] row : tempData) {
            double revenue = parseDouble(row[1]);
            double share = (revenue / totalRevenue) * 100;
            cumulativeShare += share;

            String category;
            if (cumulativeShare <= 80) category = "A";
            else if (cumulativeShare <= 95) category = "B";
            else category = "C";

            result.add(new String[]{
                    category, row[0],
                    String.format("%.2f", revenue).replace(".", ","),
                    String.format("%.1f%%", share).replace(".", ","),
                    String.format("%.1f%%", cumulativeShare).replace(".", ",")
            });
        }
        return result;
    }

    public List<String[]> getAllCategoriesFull() throws SQLException {
        List<String[]> list = new ArrayList<>();
        String sql = "SELECT id, name, COALESCE(description,'') AS description FROM categories ORDER BY name";
        try (Statement stmt = connection.createStatement();
             ResultSet rs = stmt.executeQuery(sql)) {
            while (rs.next()) {
                list.add(new String[]{String.valueOf(rs.getInt("id")), rs.getString("name"), rs.getString("description")});
            }
        }
        return list;
    }

    public void addCategory(String name, String description) throws SQLException {
        String sql = "INSERT INTO categories (name, description) VALUES (?, ?)";
        try (PreparedStatement ps = connection.prepareStatement(sql)) {
            ps.setString(1, name); ps.setString(2, description);
            ps.executeUpdate();
        }
    }

    public void updateCategory(int id, String name, String description) throws SQLException {
        String sql = "UPDATE categories SET name=?, description=? WHERE id=?";
        try (PreparedStatement ps = connection.prepareStatement(sql)) {
            ps.setString(1, name); ps.setString(2, description); ps.setInt(3, id);
            ps.executeUpdate();
        }
    }

    public void deleteCategory(int id) throws SQLException {
        String sql = "DELETE FROM categories WHERE id=? AND id NOT IN (SELECT DISTINCT category_id FROM products)";
        try (PreparedStatement ps = connection.prepareStatement(sql)) {
            ps.setInt(1, id);
            int rows = ps.executeUpdate();
            if (rows == 0) throw new SQLException("Нельзя удалить категорию, в которой есть товары");
        }
    }

    private double parseDouble(String value) {
        if (value == null) return 0.0;
        return Double.parseDouble(value.replace(",", "."));
    }

    public List<String[]> getCategoriesList() throws SQLException {
        List<String[]> list = new ArrayList<>();
        String sql = "SELECT id, name FROM categories ORDER BY name";
        try (Statement stmt = connection.createStatement();
             ResultSet rs = stmt.executeQuery(sql)) {
            while (rs.next()) {
                list.add(new String[]{String.valueOf(rs.getInt("id")), rs.getString("name")});
            }
        }
        return list;
    }

    public List<Product> getProductsFiltered(int categoryId, String searchText) throws SQLException {
        List<Product> products = new ArrayList<>();

        StringBuilder sql = new StringBuilder(
                "SELECT p.id, p.name, p.category_id, c.name AS category_name, " +
                        "p.brand, p.volume_ml, p.purchase_price, p.retail_price, " +
                        "p.stock_quantity, p.description " +
                        "FROM products p " +
                        "JOIN categories c ON p.category_id = c.id " +
                        "WHERE p.is_active = TRUE ");

        List<Object> params = new ArrayList<>();

        if (categoryId > 0) {
            sql.append("AND p.category_id = ? ");
            params.add(categoryId);
        }

        if (searchText != null && !searchText.trim().isEmpty()) {
            sql.append("AND (p.name LIKE ? OR p.brand LIKE ?) ");
            String like = "%" + searchText.trim() + "%";
            params.add(like);
            params.add(like);
        }

        sql.append("ORDER BY p.name");

        try (PreparedStatement pstmt = connection.prepareStatement(sql.toString())) {
            for (int i = 0; i < params.size(); i++) {
                pstmt.setObject(i + 1, params.get(i));
            }
            try (ResultSet rs = pstmt.executeQuery()) {
                while (rs.next()) {
                    Product product = new Product(
                            rs.getInt("id"),
                            rs.getString("name"),
                            rs.getInt("category_id"),
                            rs.getString("brand"),
                            rs.getInt("volume_ml"),
                            rs.getBigDecimal("purchase_price"),
                            rs.getBigDecimal("retail_price"),
                            rs.getInt("stock_quantity"),
                            rs.getString("description")
                    );
                    product.setCategoryName(rs.getString("category_name"));
                    products.add(product);
                }
            }
        }
        return products;
    }

    public List<Product> getProductsFiltered(int categoryId) throws SQLException {
        return getProductsFiltered(categoryId, "");
    }

    public List<Product> getProductsFiltered(String searchText) throws SQLException {
        return getProductsFiltered(0, searchText);
    }

    public void addProduct(String name, int categoryId, String brand, int volumeMl,
                           double purchasePrice, double retailPrice, int stockQuantity,
                           String description) throws SQLException {
        String sql = "INSERT INTO products (name, category_id, brand, volume_ml, " +
                "purchase_price, retail_price, stock_quantity, description) " +
                "VALUES (?, ?, ?, ?, ?, ?, ?, ?)";
        try (PreparedStatement pstmt = connection.prepareStatement(sql)) {
            pstmt.setString(1, name);
            pstmt.setInt(2, categoryId);
            pstmt.setString(3, brand);
            pstmt.setInt(4, volumeMl);
            pstmt.setDouble(5, purchasePrice);
            pstmt.setDouble(6, retailPrice);
            pstmt.setInt(7, stockQuantity);
            pstmt.setString(8, description);
            pstmt.executeUpdate();
        }
    }

    public void updateProduct(int id, String name, int categoryId, String brand,
                              int volumeMl, double purchasePrice, double retailPrice,
                              int stockQuantity, String description) throws SQLException {
        String sql = "UPDATE products SET name=?, category_id=?, brand=?, volume_ml=?, " +
                "purchase_price=?, retail_price=?, stock_quantity=?, description=? " +
                "WHERE id=?";
        try (PreparedStatement pstmt = connection.prepareStatement(sql)) {
            pstmt.setString(1, name);
            pstmt.setInt(2, categoryId);
            pstmt.setString(3, brand);
            pstmt.setInt(4, volumeMl);
            pstmt.setDouble(5, purchasePrice);
            pstmt.setDouble(6, retailPrice);
            pstmt.setInt(7, stockQuantity);
            pstmt.setString(8, description);
            pstmt.setInt(9, id);
            pstmt.executeUpdate();
        }
    }

    public List<String[]> getAllSuppliers() throws SQLException {
        List<String[]> list = new ArrayList<>();
        String sql = "SELECT id, name, COALESCE(contact_person,'') AS contact, COALESCE(phone,'') AS phone, " +
                "COALESCE(email,'') AS email, COALESCE(address,'') AS address FROM suppliers WHERE is_active = TRUE ORDER BY name";
        try (Statement stmt = connection.createStatement();
             ResultSet rs = stmt.executeQuery(sql)) {
            while (rs.next()) {
                list.add(new String[]{
                        String.valueOf(rs.getInt("id")),
                        rs.getString("name"),
                        rs.getString("contact"),
                        rs.getString("phone"),
                        rs.getString("email"),
                        rs.getString("address")
                });
            }
        }
        return list;
    }

    public List<String[]> getAllClientsWithDetails() throws SQLException {
        List<String[]> clients = new ArrayList<>();
        String sql = "SELECT id, name, phone, COALESCE(email,'') AS email, COALESCE(address,'') AS address FROM clients WHERE is_active = TRUE ORDER BY name";
        try (Statement stmt = connection.createStatement();
             ResultSet rs = stmt.executeQuery(sql)) {
            while (rs.next()) {
                clients.add(new String[]{
                        String.valueOf(rs.getInt("id")),
                        rs.getString("name"),
                        rs.getString("phone"),
                        rs.getString("email"),
                        rs.getString("address")
                });
            }
        }
        return clients;
    }

    public void deactivateProduct(int id) throws SQLException {
        String sql = "UPDATE products SET is_active = FALSE WHERE id=?";
        try (PreparedStatement pstmt = connection.prepareStatement(sql)) {
            pstmt.setInt(1, id);
            pstmt.executeUpdate();
        }
    }

    public Product getProductById(int id) throws SQLException {
        String sql = "SELECT p.*, c.name AS category_name " +
                "FROM products p JOIN categories c ON p.category_id = c.id " +
                "WHERE p.id = ?";

        try (PreparedStatement pstmt = connection.prepareStatement(sql)) {
            pstmt.setInt(1, id);
            try (ResultSet rs = pstmt.executeQuery()) {
                if (rs.next()) {
                    Product product = new Product(
                            rs.getInt("id"),
                            rs.getString("name"),
                            rs.getInt("category_id"),
                            rs.getString("brand"),
                            rs.getInt("volume_ml"),
                            rs.getBigDecimal("purchase_price"),
                            rs.getBigDecimal("retail_price"),
                            rs.getInt("stock_quantity"),
                            rs.getString("description")
                    );
                    product.setCategoryName(rs.getString("category_name"));
                    return product;
                }
            }
        }
        return null;
    }

    public void updateSupplier(int id, String name, String contact, String phone, String email, String address) throws SQLException {
        String sql = "UPDATE suppliers SET name=?, contact_person=?, phone=?, email=?, address=? WHERE id=?";
        try (PreparedStatement ps = connection.prepareStatement(sql)) {
            ps.setString(1, name); ps.setString(2, contact);
            ps.setString(3, phone); ps.setString(4, email);
            ps.setString(5, address); ps.setInt(6, id);
            ps.executeUpdate();
        }
    }

    public List<String[]> getAllUsersWithRoles() throws SQLException {
        List<String[]> list = new ArrayList<>();
        String sql = "SELECT u.id, u.login, u.full_name, r.name AS role_name, u.is_active " +
                "FROM users u JOIN roles r ON u.role_id = r.id ORDER BY u.full_name";
        try (Statement stmt = connection.createStatement();
             ResultSet rs = stmt.executeQuery(sql)) {
            while (rs.next()) {
                list.add(new String[]{
                        String.valueOf(rs.getInt("id")),
                        rs.getString("login"),
                        rs.getString("full_name"),
                        rs.getString("role_name"),
                        rs.getBoolean("is_active") ? "Активен" : "Неактивен"
                });
            }
        }
        return list;
    }

    public void addUser(String login, String password, String fullName, int roleId) throws SQLException {
        String sql = "INSERT INTO users (login, password, full_name, role_id) VALUES (?, ?, ?, ?)";
        try (PreparedStatement ps = connection.prepareStatement(sql)) {
            ps.setString(1, login);
            ps.setString(2, password);
            ps.setString(3, fullName);
            ps.setInt(4, roleId);
            ps.executeUpdate();
        }
    }

    public void updateUser(int id, String fullName, int roleId, String password) throws SQLException {
        if (password != null && !password.isEmpty()) {
            String sql = "UPDATE users SET full_name=?, role_id=?, password=? WHERE id=?";
            try (PreparedStatement ps = connection.prepareStatement(sql)) {
                ps.setString(1, fullName);
                ps.setInt(2, roleId);
                ps.setString(3, password);
                ps.setInt(4, id);
                ps.executeUpdate();
            }
        } else {
            String sql = "UPDATE users SET full_name=?, role_id=? WHERE id=?";
            try (PreparedStatement ps = connection.prepareStatement(sql)) {
                ps.setString(1, fullName);
                ps.setInt(2, roleId);
                ps.setInt(3, id);
                ps.executeUpdate();
            }
        }
    }

    public void processReturn(int saleId, int productId, int quantity, String reason) throws SQLException {
        connection.setAutoCommit(false);
        try {
            String checkSql = "SELECT COALESCE(SUM(r.quantity), 0) AS returned " +
                    "FROM returns r WHERE r.sale_id = ? AND r.product_id = ?";
            int alreadyReturned = 0;
            try (PreparedStatement ps = connection.prepareStatement(checkSql)) {
                ps.setInt(1, saleId); ps.setInt(2, productId);
                try (ResultSet rs = ps.executeQuery()) {
                    if (rs.next()) alreadyReturned = rs.getInt("returned");
                }
            }

            String soldSql = "SELECT si.quantity FROM sales_items si WHERE si.sale_id = ? AND si.product_id = ?";
            int soldQty = 0;
            try (PreparedStatement ps = connection.prepareStatement(soldSql)) {
                ps.setInt(1, saleId); ps.setInt(2, productId);
                try (ResultSet rs = ps.executeQuery()) {
                    if (rs.next()) soldQty = rs.getInt("quantity");
                }
            }

            if (alreadyReturned + quantity > soldQty) {
                throw new SQLException("Нельзя вернуть больше, чем продано. Продано: " + soldQty + ", уже возвращено: " + alreadyReturned);
            }

            String sql = "INSERT INTO returns (sale_id, product_id, quantity, reason) VALUES (?, ?, ?, ?)";
            try (PreparedStatement ps = connection.prepareStatement(sql)) {
                ps.setInt(1, saleId); ps.setInt(2, productId);
                ps.setInt(3, quantity); ps.setString(4, reason);
                ps.executeUpdate();
            }

            String updateSql = "UPDATE products SET stock_quantity = stock_quantity + ? WHERE id = ?";
            try (PreparedStatement ps = connection.prepareStatement(updateSql)) {
                ps.setInt(1, quantity); ps.setInt(2, productId);
                ps.executeUpdate();
            }

            connection.commit();
        } catch (SQLException e) {
            connection.rollback();
            throw e;
        } finally {
            connection.setAutoCommit(true);
        }
    }

    public String getReturnInfo(int saleId) throws SQLException {
        String sql = "SELECT p.name, r.quantity, r.return_date FROM returns r JOIN products p ON r.product_id = p.id WHERE r.sale_id = ?";
        StringBuilder sb = new StringBuilder();
        try (PreparedStatement ps = connection.prepareStatement(sql)) {
            ps.setInt(1, saleId);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    sb.append(rs.getString("name")).append(" x").append(rs.getInt("quantity"))
                            .append(" (").append(rs.getTimestamp("return_date").toString().substring(0, 16)).append(") | ");
                }
            }
        }
        return sb.length() > 0 ? sb.toString() : "Нет возвратов";
    }

    public List<String[]> getSaleItems(int saleId) throws SQLException {
        List<String[]> items = new ArrayList<>();
        String sql = "SELECT si.product_id, p.name, si.quantity " +
                "FROM sales_items si JOIN products p ON si.product_id = p.id " +
                "WHERE si.sale_id = ?";
        try (PreparedStatement ps = connection.prepareStatement(sql)) {
            ps.setInt(1, saleId);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    items.add(new String[]{
                            String.valueOf(rs.getInt("product_id")),
                            rs.getString("name"),
                            String.valueOf(rs.getInt("quantity"))
                    });
                }
            }
        }
        return items;
    }

    public void toggleUserStatus(int id) throws SQLException {
        String sql = "UPDATE users SET is_active = NOT is_active WHERE id=?";
        try (PreparedStatement ps = connection.prepareStatement(sql)) {
            ps.setInt(1, id);
            ps.executeUpdate();
        }
    }
}