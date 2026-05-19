package parfumery;

import javafx.scene.control.Tab;
import javafx.scene.control.TabPane;
import javafx.scene.control.TableView;
import javafx.scene.control.TableColumn;
import javafx.scene.layout.VBox;

import javafx.scene.control.Dialog;
import javafx.scene.control.ButtonType;
import javafx.scene.control.ButtonBar;
import javafx.scene.control.Alert;
import javafx.scene.control.ComboBox;

import javafx.beans.property.SimpleStringProperty;
import javafx.scene.control.CheckBox;
import javafx.scene.control.TextArea;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

import javafx.scene.chart.PieChart;

import javafx.scene.control.DatePicker;
import java.io.FileOutputStream;

import javafx.stage.FileChooser;
import java.io.File;
import java.io.PrintWriter;

import javafx.application.Application;
import javafx.application.Platform;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.HBox;
import javafx.scene.paint.Color;
import javafx.scene.text.Font;
import javafx.scene.text.FontWeight;
import javafx.scene.text.Text;
import javafx.stage.Stage;

import java.io.*;
import java.net.Socket;

public class ClientGUI extends Application {

    private static final String SERVER_ADDRESS = "localhost";
    private static final int SERVER_PORT = 8080;

    private Socket socket;
    private BufferedReader in;
    private BufferedWriter out;

    private String currentRole;
    private String currentFullName;

    private final List<ComboBox<String>> categoryCombos = new ArrayList<>();
    private final List<ComboBox<String>> clientCombos = new ArrayList<>();
    private final List<ComboBox<String>> supplierCombos = new ArrayList<>();

    public static void main(String[] args) {
        launch(args);
    }

    @Override
    public void start(Stage primaryStage) {
        primaryStage.setTitle("Парфюмерный магазин — Авторизация");
        showLoginWindow(primaryStage);
    }

    private void showLoginWindow(Stage stage) {
        VBox root = new VBox(15);
        root.setAlignment(Pos.CENTER);
        root.setPadding(new Insets(30, 40, 30, 40));
        root.setStyle("-fx-background-color: #f5f0eb;");

        Text title = new Text("Вход в систему");
        title.setFont(Font.font("System", FontWeight.BOLD, 22));
        title.setFill(Color.rgb(80, 50, 30));

        Text subtitle = new Text("Парфюмерный магазин");
        subtitle.setFont(Font.font("System", 14));
        subtitle.setFill(Color.rgb(120, 90, 60));

        GridPane grid = new GridPane();
        grid.setAlignment(Pos.CENTER);
        grid.setHgap(10);
        grid.setVgap(15);

        Label loginLabel = new Label("Логин:");
        loginLabel.setFont(Font.font("System", FontWeight.NORMAL, 13));
        loginLabel.setMinWidth(70);

        TextField loginField = new TextField();
        loginField.setPromptText("Введите логин");
        loginField.setFont(Font.font("System", 13));
        loginField.setPrefWidth(220);

        Label passLabel = new Label("Пароль:");
        passLabel.setFont(Font.font("System", FontWeight.NORMAL, 13));
        passLabel.setMinWidth(70);

        PasswordField passField = new PasswordField();
        passField.setPromptText("Введите пароль");
        passField.setFont(Font.font("System", 13));
        passField.setPrefWidth(220);

        grid.add(loginLabel, 0, 0);
        grid.add(loginField, 1, 0);
        grid.add(passLabel, 0, 1);
        grid.add(passField, 1, 1);

        Text errorText = new Text();
        errorText.setFill(Color.RED);
        errorText.setFont(Font.font("System", 12));

        Button loginBtn = new Button("Войти");
        loginBtn.setFont(Font.font("System", FontWeight.BOLD, 14));
        loginBtn.setStyle(
                "-fx-background-color: #6b4c3b; " +
                        "-fx-text-fill: white; " +
                        "-fx-padding: 10 30; " +
                        "-fx-cursor: hand;"
        );
        loginBtn.setOnMouseEntered(e -> loginBtn.setStyle(
                "-fx-background-color: #8b6c5b; -fx-text-fill: white; -fx-padding: 10 30; -fx-cursor: hand;"));
        loginBtn.setOnMouseExited(e -> loginBtn.setStyle(
                "-fx-background-color: #6b4c3b; -fx-text-fill: white; -fx-padding: 10 30; -fx-cursor: hand;"));

        loginBtn.setOnAction(e -> {
            String login = loginField.getText().trim();
            String password = passField.getText();

            if (login.isEmpty() || password.isEmpty()) {
                errorText.setText("Заполните все поля");
                return;
            }

            try {
                socket = new Socket(SERVER_ADDRESS, SERVER_PORT);
                in = new BufferedReader(new InputStreamReader(socket.getInputStream(), "UTF-8"));
                out = new BufferedWriter(new OutputStreamWriter(socket.getOutputStream(), "UTF-8"));

                out.write("LOGIN:" + login + ":" + password);
                out.newLine();
                out.flush();

                String response = in.readLine();

                if (response != null && response.startsWith("OK:")) {
                    String[] parts = response.split(":");
                    currentRole = parts[1];
                    currentFullName = parts[2];

                    in.readLine();

                    Platform.runLater(() -> {
                        stage.close();
                        showMainWindow(currentRole, currentFullName);
                    });

                } else if (response != null && response.startsWith("ERROR:")) {
                    errorText.setText(response.substring(6));
                    closeConnection();
                }

            } catch (IOException ex) {
                errorText.setText("Не удалось подключиться к серверу");
            }
        });

        passField.setOnAction(e -> loginBtn.fire());

        root.getChildren().addAll(title, subtitle, grid, errorText, loginBtn);

        Scene scene = new Scene(root, 400, 380);
        stage.setScene(scene);
        stage.setResizable(false);
        stage.show();
    }

    private void showMainWindow(String role, String fullName) {
        Stage mainStage = new Stage();
        mainStage.setTitle("Парфюмерный магазин — " + fullName + " (" + role + ")");

        TabPane tabPane = new TabPane();
        tabPane.setTabClosingPolicy(TabPane.TabClosingPolicy.UNAVAILABLE);
        tabPane.setStyle("-fx-font-family: 'System'; -fx-font-size: 13px;");

        Tab productsTab = createProductsTab(role);
        tabPane.getTabs().add(productsTab);

        if (role.equals("ADMIN") || role.equals("MANAGER")) {
            tabPane.getTabs().add(createCategoriesTab(role));

            Tab salesTab = createSalesTab(role);
            tabPane.getTabs().add(salesTab);

            Tab suppliesTab = createSuppliesTab(role);
            tabPane.getTabs().add(suppliesTab);

            Tab returnsTab = createReturnsTab();
            tabPane.getTabs().add(returnsTab);

            Tab clientsTab = createClientsTab(role);
            tabPane.getTabs().add(clientsTab);

            Tab suppliersTab = createSuppliersTabPage(role);
            tabPane.getTabs().add(suppliersTab);
        }

        Tab analyticsTab = createAnalyticsTab(role);
        tabPane.getTabs().add(analyticsTab);

        if (role.equals("ADMIN")) {
            Tab usersTab = createUsersTab();
            tabPane.getTabs().add(usersTab);
        }

        VBox root = new VBox(tabPane);
        root.setStyle("-fx-background-color: #f5f0eb;");

        Scene scene = new Scene(root, 900, 600);
        mainStage.setScene(scene);
        mainStage.show();
    }

    private Tab createProductsTab(String role) {
        Tab tab = new Tab("Товары");

        VBox vbox = new VBox(10);
        vbox.setPadding(new Insets(10));
        vbox.setStyle("-fx-background-color: #ffffff;");

        HBox filterBox = new HBox(10);
        filterBox.setAlignment(Pos.CENTER_LEFT);

        ComboBox<String> categoryFilter = new ComboBox<>();
        categoryFilter.setPromptText("Все категории");
        categoryFilter.setPrefWidth(180);
        categoryFilter.getItems().add("Все категории");

        categoryCombos.add(categoryFilter);

        TextField searchField = new TextField();
        searchField.setPromptText("Поиск");
        searchField.setPrefWidth(300);

        Button resetBtn = new Button("Сбросить");
        resetBtn.setStyle("-fx-background-color: #999; -fx-text-fill: white; -fx-cursor: hand;");

        filterBox.getChildren().addAll(
                new Label("Категория:"), categoryFilter, searchField, resetBtn);

        TableView<ProductFX> table = new TableView<>();
        table.setStyle("-fx-font-family: 'System'; -fx-font-size: 12px;");
        table.setPrefHeight(400);

        TableColumn<ProductFX, Number> idCol = new TableColumn<>("ID");
        idCol.setCellValueFactory(data -> data.getValue().idProperty());
        idCol.setPrefWidth(40);

        TableColumn<ProductFX, String> nameCol = new TableColumn<>("Название");
        nameCol.setCellValueFactory(data -> data.getValue().nameProperty());
        nameCol.setPrefWidth(170);

        TableColumn<ProductFX, String> brandCol = new TableColumn<>("Бренд");
        brandCol.setCellValueFactory(data -> data.getValue().brandProperty());
        brandCol.setPrefWidth(100);

        TableColumn<ProductFX, String> catCol = new TableColumn<>("Категория");
        catCol.setCellValueFactory(data -> data.getValue().categoryNameProperty());
        catCol.setPrefWidth(130);

        TableColumn<ProductFX, Number> volCol = new TableColumn<>("Объём");
        volCol.setCellValueFactory(data -> data.getValue().volumeMlProperty());
        volCol.setPrefWidth(60);

        TableColumn<ProductFX, Number> purchPriceCol = new TableColumn<>("Закуп. цена");
        purchPriceCol.setCellValueFactory(data -> data.getValue().purchasePriceProperty());
        purchPriceCol.setPrefWidth(90);

        TableColumn<ProductFX, Number> priceCol = new TableColumn<>("Розн. цена");
        priceCol.setCellValueFactory(data -> data.getValue().retailPriceProperty());
        priceCol.setPrefWidth(90);

        TableColumn<ProductFX, Number> stockCol = new TableColumn<>("Остаток");
        stockCol.setCellValueFactory(data -> data.getValue().stockQuantityProperty());
        stockCol.setPrefWidth(60);

        TableColumn<ProductFX, String> descCol = new TableColumn<>("Описание");
        descCol.setCellValueFactory(data -> data.getValue().descriptionProperty());
        descCol.setPrefWidth(150);

        table.getColumns().addAll(idCol, nameCol, brandCol, catCol, volCol,
                purchPriceCol, priceCol, stockCol, descCol);

        HBox crudBox = new HBox(10);
        crudBox.setAlignment(Pos.CENTER_LEFT);

        Button refreshBtn = new Button("Обновить");
        refreshBtn.setStyle("-fx-background-color: #6b4c3b; -fx-text-fill: white; -fx-cursor: hand;");

        Button addBtn = new Button("Добавить");
        addBtn.setStyle("-fx-background-color: #4a7c3f; -fx-text-fill: white; -fx-cursor: hand;");

        Button editBtn = new Button("Редактировать");
        editBtn.setStyle("-fx-background-color: #c49b2a; -fx-text-fill: white; -fx-cursor: hand;");

        Button deleteBtn = new Button("Удалить");
        deleteBtn.setStyle("-fx-background-color: #b53b3b; -fx-text-fill: white; -fx-cursor: hand;");

        crudBox.getChildren().addAll(refreshBtn, addBtn, editBtn, deleteBtn);

        if (!role.equals("ADMIN")) deleteBtn.setVisible(false);
        if (role.equals("ANALYST")) {
            addBtn.setVisible(false);
            editBtn.setVisible(false);
            deleteBtn.setVisible(false);
        }

        loadCategories(categoryFilter);

        Runnable applyFilter = () -> {
            table.getItems().clear();
            int catId = getSelectedCategoryId(categoryFilter);
            String search = searchField.getText().trim();

            if (catId == 0 && search.isEmpty()) {
                loadProducts(table);
            } else if (catId == 0 && !search.isEmpty()) {
                loadAllAndFilter(table, search);
            } else {
                loadFilteredProducts(table, catId, search);
            }
        };

        searchField.textProperty().addListener((obs, oldVal, newVal) -> applyFilter.run());

        categoryFilter.setOnAction(e -> applyFilter.run());

        resetBtn.setOnAction(e -> {
            categoryFilter.getSelectionModel().select(0);
            searchField.clear();
        });

        refreshBtn.setOnAction(e -> applyFilter.run());

        addBtn.setOnAction(e -> showAddProductDialog(table));
        editBtn.setOnAction(e -> showEditProductDialog(table));
        deleteBtn.setOnAction(e -> deactivateProduct(table));

        loadProducts(table);

        vbox.getChildren().addAll(filterBox, table, crudBox);
        tab.setContent(vbox);

        return tab;
    }

    private void loadAllAndFilter(TableView<ProductFX> table, String search) {
        String lowerSearch = search.toLowerCase();

        try {
            out.write("LIST");
            out.newLine();
            out.flush();

            String line;
            while ((line = in.readLine()) != null) {
                if (line.equals("===END===")) break;

                try {
                    if (line.startsWith("===") || line.startsWith("Всего")) continue;

                    String s = line;
                    if (s.startsWith("[") && s.contains("]")) {
                        int idEnd = s.indexOf("]");
                        int id = Integer.parseInt(s.substring(1, idEnd));

                        String data = s.substring(idEnd + 2);
                        String[] parts = data.split("\\|");

                        if (parts.length >= 8) {
                            String name = parts[0];
                            String brand = parts[1].equals("-") ? "" : parts[1];
                            int volume = Integer.parseInt(parts[2]);
                            String cat = parts[3];
                            double purchPrice = Double.parseDouble(parts[4].replace(",", "."));
                            double retailPrice = Double.parseDouble(parts[5].replace(",", "."));
                            int stock = Integer.parseInt(parts[6]);
                            String desc = parts[7];

                            if (name.toLowerCase().contains(lowerSearch) ||
                                    brand.toLowerCase().contains(lowerSearch) ||
                                    cat.toLowerCase().contains(lowerSearch) ||
                                    desc.toLowerCase().contains(lowerSearch) ||
                                    String.valueOf(id).contains(lowerSearch) ||
                                    String.valueOf(volume).contains(lowerSearch) ||
                                    parts[5].contains(lowerSearch) ||
                                    String.valueOf(stock).contains(lowerSearch)) {

                                table.getItems().add(new ProductFX(id, name, brand, cat, volume,
                                        purchPrice, retailPrice, stock, desc));
                            }
                        }
                    }
                } catch (Exception ex) {
                    System.err.println("Ошибка парсинга: " + line);
                }
            }
        } catch (IOException ex) {
            System.err.println("Ошибка загрузки: " + ex.getMessage());
        }
    }

    private Tab createSalesTab(String role) {
        Tab tab = new Tab("Продажи");

        TabPane innerPane = new TabPane();
        innerPane.setTabClosingPolicy(TabPane.TabClosingPolicy.UNAVAILABLE);

        Tab newSaleTab = new Tab("Новая продажа");
        newSaleTab.setContent(createNewSaleForm());

        Tab historyTab = new Tab("История продаж");
        historyTab.setContent(createSalesHistory());

        innerPane.getTabs().addAll(newSaleTab, historyTab);

        tab.setContent(innerPane);
        return tab;
    }

    private VBox createNewSaleForm() {
        VBox vbox = new VBox(10);
        vbox.setPadding(new Insets(15));
        vbox.setStyle("-fx-background-color: #ffffff;");

        Text header = new Text("Оформление новой продажи");
        header.setFont(Font.font("System", FontWeight.BOLD, 16));
        header.setFill(Color.rgb(80, 50, 30));

        final List<String[]> checkItems = new ArrayList<>();
        final double[] totalSum = {0.0};

        HBox clientBox = new HBox(10);
        clientBox.setAlignment(Pos.CENTER_LEFT);

        ComboBox<String> clientCombo = new ComboBox<>();
        clientCombo.setPromptText("Выберите клиента");
        clientCombo.setPrefWidth(300);
        clientCombo.setEditable(true);

        clientCombos.add(clientCombo);

        Button addClientBtn = new Button("+");
        addClientBtn.setStyle("-fx-background-color: #4a7c3f; -fx-text-fill: white; -fx-font-weight: bold; -fx-cursor: hand;");
        addClientBtn.setOnAction(e -> showAddClientFromSaleDialog(clientCombo));

        CheckBox noClientCheck = new CheckBox("Без клиента");
        noClientCheck.setOnAction(e -> {
            clientCombo.setDisable(noClientCheck.isSelected());
            addClientBtn.setDisable(noClientCheck.isSelected());
        });

        loadClients(clientCombo);

        clientBox.getChildren().addAll(new Label("Клиент:"), clientCombo, addClientBtn, noClientCheck);

        TableView<ProductFX> saleTable = new TableView<>();
        saleTable.setPrefHeight(200);

        TableColumn<ProductFX, Number> sIdCol = new TableColumn<>("ID");
        sIdCol.setCellValueFactory(data -> data.getValue().idProperty());
        sIdCol.setPrefWidth(40);

        TableColumn<ProductFX, String> sNameCol = new TableColumn<>("Товар");
        sNameCol.setCellValueFactory(data -> data.getValue().nameProperty());
        sNameCol.setPrefWidth(200);

        TableColumn<ProductFX, Number> sPriceCol = new TableColumn<>("Цена");
        sPriceCol.setCellValueFactory(data -> data.getValue().retailPriceProperty());
        sPriceCol.setPrefWidth(80);

        TableColumn<ProductFX, Number> sStockCol = new TableColumn<>("Остаток");
        sStockCol.setCellValueFactory(data -> data.getValue().stockQuantityProperty());
        sStockCol.setPrefWidth(60);

        saleTable.getColumns().addAll(sIdCol, sNameCol, sPriceCol, sStockCol);
        loadProductsForSale(saleTable);

        HBox addItemBox = new HBox(10);
        addItemBox.setAlignment(Pos.CENTER_LEFT);

        TextField quantityField = new TextField();
        quantityField.setPromptText("Кол-во");
        quantityField.setPrefWidth(60);

        TextField customPriceField = new TextField();
        customPriceField.setPromptText("Цена (если отличается)");
        customPriceField.setPrefWidth(150);

        Button addItemBtn = new Button("Добавить в чек");
        addItemBtn.setStyle("-fx-background-color: #4a7c3f; -fx-text-fill: white; -fx-cursor: hand;");

        Button refreshSaleProductsBtn = new Button("Обновить");
        refreshSaleProductsBtn.setStyle("-fx-background-color: #999; -fx-text-fill: white; -fx-cursor: hand;");
        refreshSaleProductsBtn.setOnAction(e -> loadProductsForSale(saleTable));

        addItemBox.getChildren().addAll(
                new Label("Кол-во:"), quantityField,
                new Label("Цена:"), customPriceField,
                addItemBtn, refreshSaleProductsBtn);

        TableView<String[]> checkTable = new TableView<>();
        checkTable.setPrefHeight(150);

        TableColumn<String[], String> cNameCol = new TableColumn<>("Товар");
        cNameCol.setCellValueFactory(data -> new SimpleStringProperty(data.getValue()[1]));
        cNameCol.setPrefWidth(250);

        TableColumn<String[], String> cQtyCol = new TableColumn<>("Кол-во");
        cQtyCol.setCellValueFactory(data -> new SimpleStringProperty(data.getValue()[2]));
        cQtyCol.setPrefWidth(60);

        TableColumn<String[], String> cPriceCol = new TableColumn<>("Цена");
        cPriceCol.setCellValueFactory(data -> new SimpleStringProperty(data.getValue()[3]));
        cPriceCol.setPrefWidth(80);

        TableColumn<String[], String> cSumCol = new TableColumn<>("Сумма");
        cSumCol.setCellValueFactory(data -> new SimpleStringProperty(data.getValue()[4]));
        cSumCol.setPrefWidth(80);

        checkTable.getColumns().addAll(cNameCol, cQtyCol, cPriceCol, cSumCol);

        Text totalText = new Text("Итого: 0,00 руб.");
        totalText.setFont(Font.font("System", FontWeight.BOLD, 16));
        totalText.setFill(Color.rgb(80, 50, 30));

        addItemBtn.setOnAction(e -> {
            ProductFX selected = saleTable.getSelectionModel().getSelectedItem();
            if (selected == null) {
                showAlert("Ошибка", "Выберите товар из таблицы");
                return;
            }

            int qty;
            try {
                qty = Integer.parseInt(quantityField.getText().trim());
                if (qty <= 0) throw new NumberFormatException();
            } catch (NumberFormatException ex) {
                showAlert("Ошибка", "Введите корректное количество");
                return;
            }

            if (qty > selected.getStockQuantity()) {
                showAlert("Ошибка", "Недостаточно товара на складе! Остаток: " + selected.getStockQuantity());
                return;
            }

            double price = selected.getRetailPrice();
            if (!customPriceField.getText().trim().isEmpty()) {
                try {
                    price = Double.parseDouble(customPriceField.getText().trim().replace(",", "."));
                } catch (NumberFormatException ex) {
                    showAlert("Ошибка", "Неверный формат цены");
                    return;
                }
            }

            double sum = qty * price;
            totalSum[0] += sum;

            String[] item = new String[]{
                    String.valueOf(selected.getId()),
                    selected.getName(),
                    String.valueOf(qty),
                    String.format("%.2f", price).replace(",", "."),
                    String.format("%.2f", sum).replace(",", ".")
            };

            checkItems.add(item);
            checkTable.getItems().add(item);
            totalText.setText(String.format("Итого: %.2f руб.", totalSum[0]));

            quantityField.clear();
            customPriceField.clear();
        });

        Button removeItemBtn = new Button("Удалить позицию");
        removeItemBtn.setStyle("-fx-background-color: #b53b3b; -fx-text-fill: white; -fx-cursor: hand;");
        removeItemBtn.setOnAction(e -> {
            String[] selected = checkTable.getSelectionModel().getSelectedItem();
            if (selected == null) {
                showAlert("Ошибка", "Выберите позицию для удаления");
                return;
            }
            double sumToRemove = Double.parseDouble(selected[4].replace(",", "."));
            totalSum[0] -= sumToRemove;

            checkItems.remove(selected);
            checkTable.getItems().remove(selected);
            totalText.setText(String.format("Итого: %.2f руб.", totalSum[0]));
        });

        Button completeSaleBtn = new Button("Оформить продажу");
        completeSaleBtn.setStyle("-fx-background-color: #6b4c3b; -fx-text-fill: white; -fx-font-size: 14px; -fx-padding: 10 20; -fx-cursor: hand;");
        completeSaleBtn.setOnAction(e -> {
            if (checkItems.isEmpty()) {
                showAlert("Ошибка", "Добавьте хотя бы один товар в чек");
                return;
            }

            try {
                int clientId = 0;
                if (!noClientCheck.isSelected()) {
                    String selected = clientCombo.getSelectionModel().getSelectedItem();
                    if (selected != null && selected.contains("[")) {
                        int start = selected.lastIndexOf("[");
                        int end = selected.lastIndexOf("]");
                        if (start >= 0 && end > start) {
                            clientId = Integer.parseInt(selected.substring(start + 1, end));
                        }
                    }
                }

                StringBuilder cmd = new StringBuilder("SALE:").append(clientId).append(":");
                for (int i = 0; i < checkItems.size(); i++) {
                    if (i > 0) cmd.append(",");
                    String[] item = checkItems.get(i);
                    cmd.append(item[0]).append(":").append(item[2]).append(":").append(item[3]);
                }

                out.write(cmd.toString());
                out.newLine();
                out.flush();

                String response = in.readLine();
                in.readLine();

                if (response.contains("успешно")) {
                    showAlert("Успех", "Продажа оформлена!");
                    checkItems.clear();
                    checkTable.getItems().clear();
                    totalSum[0] = 0;
                    totalText.setText("Итого: 0,00 руб.");
                    loadProductsForSale(saleTable);
                } else {
                    showAlert("Ошибка", response);
                }
            } catch (IOException ex) {
                showAlert("Ошибка", "Ошибка связи с сервером");
            }
        });

        HBox checkButtons = new HBox(10, removeItemBtn, completeSaleBtn);
        checkButtons.setAlignment(Pos.CENTER_LEFT);

        vbox.getChildren().addAll(header, clientBox, new Label("Товары в наличии:"), saleTable,
                addItemBox, new Label("Текущий чек:"), checkTable, totalText, checkButtons);

        return vbox;
    }

    private VBox createSalesHistory() {
        VBox vbox = new VBox(10);
        vbox.setPadding(new Insets(15));
        vbox.setStyle("-fx-background-color: #ffffff;");

        Text header = new Text("История продаж");
        header.setFont(Font.font("System", FontWeight.BOLD, 16));
        header.setFill(Color.rgb(80, 50, 30));

        HBox filterRow1 = new HBox(10);
        filterRow1.setAlignment(Pos.CENTER_LEFT);

        TextField searchField = new TextField();
        searchField.setPromptText("Поиск");
        searchField.setPrefWidth(200);

        DatePicker fromDate = new DatePicker(LocalDate.now().minusDays(30));
        DatePicker toDate = new DatePicker(LocalDate.now());

        Button filterDateBtn = new Button("Фильтр");
        filterDateBtn.setStyle("-fx-background-color: #6b4c3b; -fx-text-fill: white; -fx-cursor: hand;");

        Button resetBtn = new Button("Сбросить");
        resetBtn.setStyle("-fx-background-color: #999; -fx-text-fill: white; -fx-cursor: hand;");

        Button exportWordBtn = new Button("Экспорт в Word");
        exportWordBtn.setStyle("-fx-background-color: #4a7c3f; -fx-text-fill: white; -fx-cursor: hand;");

        filterRow1.getChildren().addAll(
                new Label("Поиск:"), searchField,
                new Label("С:"), fromDate, new Label("По:"), toDate,
                filterDateBtn, resetBtn, exportWordBtn
        );

        HBox filterRow2 = new HBox(10);
        filterRow2.setAlignment(Pos.CENTER_LEFT);

        TextField sumFromField = new TextField();
        sumFromField.setPromptText("Сумма от");
        sumFromField.setPrefWidth(100);

        TextField sumToField = new TextField();
        sumToField.setPromptText("до");
        sumToField.setPrefWidth(100);

        filterRow2.getChildren().addAll(
                new Label("Сумма:"), sumFromField, sumToField
        );

        VBox filterBox = new VBox(5, filterRow1, filterRow2);

        TableView<SaleFX> table = new TableView<>();
        table.setPrefHeight(350);
        table.setStyle("-fx-font-family: 'System'; -fx-font-size: 12px;");

        TableColumn<SaleFX, Number> idCol = new TableColumn<>("№");
        idCol.setCellValueFactory(data -> data.getValue().idProperty());
        idCol.setPrefWidth(50);

        TableColumn<SaleFX, String> dateCol = new TableColumn<>("Дата");
        dateCol.setCellValueFactory(data -> data.getValue().dateProperty());
        dateCol.setPrefWidth(130);

        TableColumn<SaleFX, String> clientCol = new TableColumn<>("Клиент");
        clientCol.setCellValueFactory(data -> data.getValue().clientProperty());
        clientCol.setPrefWidth(150);

        TableColumn<SaleFX, String> sellerCol = new TableColumn<>("Продавец");
        sellerCol.setCellValueFactory(data -> data.getValue().sellerProperty());
        sellerCol.setPrefWidth(150);

        TableColumn<SaleFX, Number> amountCol = new TableColumn<>("Сумма");
        amountCol.setCellValueFactory(data -> data.getValue().totalAmountProperty());
        amountCol.setPrefWidth(100);

        TableColumn<SaleFX, String> itemsCol = new TableColumn<>("Товары");
        itemsCol.setCellValueFactory(data -> data.getValue().itemsProperty());
        itemsCol.setPrefWidth(300);

        table.getColumns().addAll(idCol, dateCol, clientCol, sellerCol, amountCol, itemsCol);

        exportWordBtn.setOnAction(e -> {
            SaleFX selected = table.getSelectionModel().getSelectedItem();
            if (selected == null) {
                showAlert("Ошибка", "Выберите строку для экспорта");
                return;
            }
            exportSaleToDocx(selected);
        });

        List<SaleFX> allSales = new ArrayList<>();

        Runnable applySaleFilter = () -> {
            String search = searchField.getText().trim().toLowerCase();
            table.getItems().clear();
            for (SaleFX s : allSales) {
                if (fromDate.getValue() != null && toDate.getValue() != null) {
                    String saleDate = s.getDate().length() >= 10 ? s.getDate().substring(0, 10) : "";
                    if (saleDate.compareTo(fromDate.getValue().toString()) < 0 ||
                            saleDate.compareTo(toDate.getValue().toString()) > 0) continue;
                }
                if (!sumFromField.getText().trim().isEmpty()) {
                    try {
                        double minSum = Double.parseDouble(sumFromField.getText().trim().replace(",", "."));
                        if (s.getTotalAmount() < minSum) continue;
                    } catch (NumberFormatException ignored) {}
                }
                if (!sumToField.getText().trim().isEmpty()) {
                    try {
                        double maxSum = Double.parseDouble(sumToField.getText().trim().replace(",", "."));
                        if (s.getTotalAmount() > maxSum) continue;
                    } catch (NumberFormatException ignored) {}
                }
                if (!search.isEmpty()) {
                    if (!s.getClient().toLowerCase().contains(search) &&
                            !s.getSeller().toLowerCase().contains(search) &&
                            !s.getItems().toLowerCase().contains(search)) continue;
                }
                table.getItems().add(s);
            }
        };

        searchField.textProperty().addListener((obs, old, val) -> applySaleFilter.run());
        filterDateBtn.setOnAction(e -> applySaleFilter.run());

        resetBtn.setOnAction(e -> {
            searchField.clear();
            fromDate.setValue(LocalDate.now().minusDays(30));
            toDate.setValue(LocalDate.now());
            sumFromField.clear();
            sumToField.clear();
            applySaleFilter.run();
        });

        Runnable loadSales = () -> {
            allSales.clear();
            try {
                out.write("SALES_HISTORY");
                out.newLine(); out.flush();
                String line;
                while ((line = in.readLine()) != null) {
                    if (line.equals("===END===")) break;
                    try {
                        if (line.startsWith("[Продажа #")) {
                            int idStart = line.indexOf("#") + 1;
                            int idEnd = line.indexOf("]");
                            int id = Integer.parseInt(line.substring(idStart, idEnd));
                            String afterId = line.substring(idEnd + 2);
                            String[] parts = afterId.split(" \\| ");
                            String date = parts[0].trim();
                            String client = parts.length > 1 ? parts[1].replace("Клиент: ", "").trim() : "";
                            String seller = parts.length > 2 ? parts[2].replace("Продавец: ", "").trim() : "";
                            double amount = 0; String items = "";
                            if (parts.length > 3) {
                                amount = Double.parseDouble(parts[3].replace("Сумма: ", "").replace(" руб.", "").replace(",", ".").trim());
                            }
                            if (parts.length > 4) items = parts[4].replace("Товары: ", "").trim();
                            allSales.add(new SaleFX(id, date, client, seller, amount, items));
                        }
                    } catch (Exception ex) {}
                }
                applySaleFilter.run();
            } catch (IOException ex) {}
        };

        Button refreshBtn = new Button("Обновить");
        refreshBtn.setStyle("-fx-background-color: #6b4c3b; -fx-text-fill: white; -fx-cursor: hand;");
        refreshBtn.setOnAction(e -> loadSales.run());

        loadSales.run();

        vbox.getChildren().addAll(header, filterBox, table, refreshBtn);
        return vbox;
    }

    private void exportSaleToDocx(SaleFX sale) {
        FileChooser fileChooser = new FileChooser();
        fileChooser.setTitle("Сохранить документ о продаже");
        fileChooser.getExtensionFilters().add(new FileChooser.ExtensionFilter("Word Document", "*.doc"));
        fileChooser.setInitialFileName("Продажа_№" + sale.getId() + ".doc");

        File file = fileChooser.showSaveDialog(null);
        if (file != null) {
            try {
                WordExporter.exportSaleToWord(sale, currentFullName, file);
                showAlert("Экспорт", "Документ сохранён!\n" + file.getName() + "\nОткройте в Microsoft Word.");
            } catch (IOException ex) {
                showAlert("Ошибка", "Не удалось сохранить файл");
            }
        }
    }

    private void loadClients(ComboBox<String> combo) {
        try {
            out.write("CLIENTS");
            out.newLine(); out.flush();

            String line;
            while ((line = in.readLine()) != null) {
                if (line.equals("===END===")) break;
                String[] parts = line.split("\\|");
                if (parts.length >= 2) {
                    combo.getItems().add(parts[1] + " (" + parts[2] + ") [" + parts[0] + "]");
                }
            }
        } catch (IOException ex) {
            System.err.println("Ошибка загрузки клиентов");
        }
    }

    private void loadProductsForSale(TableView<ProductFX> table) {
        table.getItems().clear();
        try {
            out.write("LIST");
            out.newLine();
            out.flush();
            parseProductsFromServer(table);
        } catch (IOException ex) {
            System.err.println("Ошибка загрузки товаров");
        }
    }

    private Tab createSuppliesTab(String role) {
        Tab tab = new Tab("Поставки");

        TabPane innerPane = new TabPane();
        innerPane.setTabClosingPolicy(TabPane.TabClosingPolicy.UNAVAILABLE);

        Tab newSupplyTab = new Tab("Новая поставка");
        newSupplyTab.setContent(createNewSupplyForm());

        Tab historyTab = new Tab("История поставок");
        historyTab.setContent(createSuppliesHistory());

        innerPane.getTabs().addAll(newSupplyTab, historyTab);
        tab.setContent(innerPane);
        return tab;
    }

    private VBox createNewSupplyForm() {
        VBox vbox = new VBox(10);
        vbox.setPadding(new Insets(15));
        vbox.setStyle("-fx-background-color: #ffffff;");

        Text header = new Text("Оформление новой поставки");
        header.setFont(Font.font("System", FontWeight.BOLD, 16));
        header.setFill(Color.rgb(80, 50, 30));

        final List<String[]> supplyItems = new ArrayList<>();
        final double[] totalSum = {0.0};

        HBox supplierBox = new HBox(10);
        supplierBox.setAlignment(Pos.CENTER_LEFT);

        ComboBox<String> supplierCombo = new ComboBox<>();
        supplierCombo.setPromptText("Выберите поставщика");
        supplierCombo.setPrefWidth(300);
        supplierCombo.setEditable(true);

        supplierCombos.add(supplierCombo);

        Button addSupplierBtn = new Button("+");
        addSupplierBtn.setStyle("-fx-background-color: #4a7c3f; -fx-text-fill: white; -fx-font-weight: bold; -fx-cursor: hand;");
        addSupplierBtn.setOnAction(e -> showAddSupplierDialog(supplierCombo));

        CheckBox noSupplierCheck = new CheckBox("Без поставщика");
        noSupplierCheck.setOnAction(e -> {
            supplierCombo.setDisable(noSupplierCheck.isSelected());
            addSupplierBtn.setDisable(noSupplierCheck.isSelected());
        });

        loadSuppliers(supplierCombo);

        supplierBox.getChildren().addAll(new Label("Поставщик:"), supplierCombo, addSupplierBtn, noSupplierCheck);

        TableView<ProductFX> productTable = new TableView<>();
        productTable.setPrefHeight(200);

        TableColumn<ProductFX, Number> pIdCol = new TableColumn<>("ID");
        pIdCol.setCellValueFactory(data -> data.getValue().idProperty());
        pIdCol.setPrefWidth(40);

        TableColumn<ProductFX, String> pNameCol = new TableColumn<>("Товар");
        pNameCol.setCellValueFactory(data -> data.getValue().nameProperty());
        pNameCol.setPrefWidth(200);

        TableColumn<ProductFX, Number> pPriceCol = new TableColumn<>("Закуп. цена");
        pPriceCol.setCellValueFactory(data -> data.getValue().purchasePriceProperty());
        pPriceCol.setPrefWidth(90);

        TableColumn<ProductFX, Number> pStockCol = new TableColumn<>("Остаток");
        pStockCol.setCellValueFactory(data -> data.getValue().stockQuantityProperty());
        pStockCol.setPrefWidth(60);

        productTable.getColumns().addAll(pIdCol, pNameCol, pPriceCol, pStockCol);
        loadProductsForSale(productTable);

        HBox addItemBox = new HBox(10);
        addItemBox.setAlignment(Pos.CENTER_LEFT);

        TextField qtyField = new TextField();
        qtyField.setPromptText("Кол-во");
        qtyField.setPrefWidth(60);

        TextField priceField = new TextField();
        priceField.setPromptText("Закупочная цена");
        priceField.setPrefWidth(120);

        Button addItemBtn = new Button("Добавить в поставку");
        addItemBtn.setStyle("-fx-background-color: #4a7c3f; -fx-text-fill: white; -fx-cursor: hand;");

        Button refreshSupplyProductsBtn = new Button("Обновить");
        refreshSupplyProductsBtn.setStyle("-fx-background-color: #999; -fx-text-fill: white; -fx-cursor: hand;");
        refreshSupplyProductsBtn.setOnAction(e -> loadProductsForSale(productTable));

        addItemBox.getChildren().addAll(
                new Label("Кол-во:"), qtyField,
                new Label("Цена:"), priceField,
                addItemBtn, refreshSupplyProductsBtn);

        TableView<String[]> supplyTable = new TableView<>();
        supplyTable.setPrefHeight(150);

        TableColumn<String[], String> sNameCol = new TableColumn<>("Товар");
        sNameCol.setCellValueFactory(data -> new SimpleStringProperty(data.getValue()[1]));
        sNameCol.setPrefWidth(250);

        TableColumn<String[], String> sQtyCol = new TableColumn<>("Кол-во");
        sQtyCol.setCellValueFactory(data -> new SimpleStringProperty(data.getValue()[2]));
        sQtyCol.setPrefWidth(60);

        TableColumn<String[], String> sPriceCol = new TableColumn<>("Цена");
        sPriceCol.setCellValueFactory(data -> new SimpleStringProperty(data.getValue()[3]));
        sPriceCol.setPrefWidth(80);

        TableColumn<String[], String> sSumCol = new TableColumn<>("Сумма");
        sSumCol.setCellValueFactory(data -> new SimpleStringProperty(data.getValue()[4]));
        sSumCol.setPrefWidth(80);

        supplyTable.getColumns().addAll(sNameCol, sQtyCol, sPriceCol, sSumCol);

        Text totalText = new Text("Итого: 0,00 руб.");
        totalText.setFont(Font.font("System", FontWeight.BOLD, 16));
        totalText.setFill(Color.rgb(80, 50, 30));

        addItemBtn.setOnAction(e -> {
            ProductFX selected = productTable.getSelectionModel().getSelectedItem();
            if (selected == null) {
                showAlert("Ошибка", "Выберите товар");
                return;
            }

            int qty;
            double price;
            try {
                qty = Integer.parseInt(qtyField.getText().trim());
                if (qty <= 0) throw new NumberFormatException();
                price = Double.parseDouble(priceField.getText().trim().replace(",", "."));
            } catch (NumberFormatException ex) {
                showAlert("Ошибка", "Введите корректные количество и цену");
                return;
            }

            double sum = qty * price;
            totalSum[0] += sum;

            String[] item = new String[]{
                    String.valueOf(selected.getId()),
                    selected.getName(),
                    String.valueOf(qty),
                    String.format("%.2f", price).replace(",", "."),
                    String.format("%.2f", sum).replace(",", ".")
            };

            supplyItems.add(item);
            supplyTable.getItems().add(item);
            totalText.setText(String.format("Итого: %.2f руб.", totalSum[0]));

            qtyField.clear();
            priceField.clear();
        });

        Button removeItemBtn = new Button("Удалить позицию");
        removeItemBtn.setStyle("-fx-background-color: #b53b3b; -fx-text-fill: white; -fx-cursor: hand;");
        removeItemBtn.setOnAction(e -> {
            String[] selected = supplyTable.getSelectionModel().getSelectedItem();
            if (selected == null) {
                showAlert("Ошибка", "Выберите позицию для удаления");
                return;
            }
            double sumToRemove = Double.parseDouble(selected[4].replace(",", "."));
            totalSum[0] -= sumToRemove;

            supplyItems.remove(selected);
            supplyTable.getItems().remove(selected);
            totalText.setText(String.format("Итого: %.2f руб.", totalSum[0]));
        });

        Button completeBtn = new Button("Оформить поставку");
        completeBtn.setStyle("-fx-background-color: #6b4c3b; -fx-text-fill: white; -fx-font-size: 14px; -fx-padding: 10 20; -fx-cursor: hand;");
        completeBtn.setOnAction(e -> {
            if (supplyItems.isEmpty()) {
                showAlert("Ошибка", "Добавьте хотя бы один товар");
                return;
            }

            int supplierId = 0;
            String supplierName;
            if (noSupplierCheck.isSelected()) {
                supplierName = "Без поставщика";
            } else {
                supplierName = supplierCombo.getEditor().getText().trim();
                if (supplierName.isEmpty()) {
                    showAlert("Ошибка", "Введите или выберите поставщика");
                    return;
                }
                String selected = supplierCombo.getSelectionModel().getSelectedItem();
                if (selected != null && selected.contains("[")) {
                    int start = selected.lastIndexOf("[");
                    int end = selected.lastIndexOf("]");
                    if (start >= 0 && end > start) {
                        supplierId = Integer.parseInt(selected.substring(start + 1, end));
                    }
                }
            }

            try {
                StringBuilder cmd = new StringBuilder("SUPPLY:")
                        .append(supplierId).append(":")
                        .append(supplierName).append(":");
                for (int i = 0; i < supplyItems.size(); i++) {
                    if (i > 0) cmd.append(",");
                    String[] item = supplyItems.get(i);
                    cmd.append(item[0]).append(":").append(item[2]).append(":").append(item[3]);
                }

                out.write(cmd.toString());
                out.newLine();
                out.flush();

                String response = in.readLine();
                in.readLine();

                if (response.contains("успешно")) {
                    showAlert("Успех", "Поставка оформлена!");
                    supplyItems.clear();
                    supplyTable.getItems().clear();
                    totalSum[0] = 0;
                    totalText.setText("Итого: 0,00 руб.");
                    loadProductsForSale(productTable);
                } else {
                    showAlert("Ошибка", response);
                }
            } catch (IOException ex) {
                showAlert("Ошибка", "Ошибка связи с сервером");
            }
        });

        HBox supplyButtons = new HBox(10, removeItemBtn, completeBtn);
        supplyButtons.setAlignment(Pos.CENTER_LEFT);

        vbox.getChildren().addAll(header, supplierBox, new Label("Товары:"), productTable,
                addItemBox, new Label("Текущая поставка:"), supplyTable, totalText, supplyButtons);

        return vbox;
    }

    private VBox createSuppliesHistory() {
        VBox vbox = new VBox(10);
        vbox.setPadding(new Insets(15));
        vbox.setStyle("-fx-background-color: #ffffff;");

        Text header = new Text("История поставок");
        header.setFont(Font.font("System", FontWeight.BOLD, 16));
        header.setFill(Color.rgb(80, 50, 30));

        HBox filterRow1 = new HBox(10);
        filterRow1.setAlignment(Pos.CENTER_LEFT);

        TextField searchField = new TextField();
        searchField.setPromptText("Поиск по поставщику или товарам");
        searchField.setPrefWidth(200);

        DatePicker fromDate = new DatePicker(LocalDate.now().minusDays(30));
        DatePicker toDate = new DatePicker(LocalDate.now());

        Button filterDateBtn = new Button("Фильтр");
        filterDateBtn.setStyle("-fx-background-color: #6b4c3b; -fx-text-fill: white; -fx-cursor: hand;");

        Button resetBtn = new Button("Сбросить");
        resetBtn.setStyle("-fx-background-color: #999; -fx-text-fill: white; -fx-cursor: hand;");

        Button exportWordBtn = new Button("Экспорт в Word");
        exportWordBtn.setStyle("-fx-background-color: #4a7c3f; -fx-text-fill: white; -fx-cursor: hand;");

        filterRow1.getChildren().addAll(
                new Label("Поиск:"), searchField,
                new Label("С:"), fromDate, new Label("По:"), toDate,
                filterDateBtn, resetBtn, exportWordBtn
        );

        HBox filterRow2 = new HBox(10);
        filterRow2.setAlignment(Pos.CENTER_LEFT);

        TextField sumFromField = new TextField();
        sumFromField.setPromptText("Сумма от");
        sumFromField.setPrefWidth(100);

        TextField sumToField = new TextField();
        sumToField.setPromptText("до");
        sumToField.setPrefWidth(100);

        filterRow2.getChildren().addAll(
                new Label("Сумма:"), sumFromField, sumToField
        );

        VBox filterBox = new VBox(5, filterRow1, filterRow2);

        // Таблица
        TableView<SupplyFX> table = new TableView<>();
        table.setPrefHeight(380);
        table.setStyle("-fx-font-family: 'System'; -fx-font-size: 12px;");

        TableColumn<SupplyFX, Number> idCol = new TableColumn<>("№");
        idCol.setCellValueFactory(data -> data.getValue().idProperty());
        idCol.setPrefWidth(50);

        TableColumn<SupplyFX, String> dateCol = new TableColumn<>("Дата");
        dateCol.setCellValueFactory(data -> data.getValue().dateProperty());
        dateCol.setPrefWidth(130);

        TableColumn<SupplyFX, String> supplierCol = new TableColumn<>("Поставщик");
        supplierCol.setCellValueFactory(data -> data.getValue().supplierProperty());
        supplierCol.setPrefWidth(180);

        TableColumn<SupplyFX, String> receiverCol = new TableColumn<>("Принял");
        receiverCol.setCellValueFactory(data -> data.getValue().receiverProperty());
        receiverCol.setPrefWidth(150);

        TableColumn<SupplyFX, Number> amountCol = new TableColumn<>("Сумма");
        amountCol.setCellValueFactory(data -> data.getValue().totalAmountProperty());
        amountCol.setPrefWidth(100);

        TableColumn<SupplyFX, String> itemsCol = new TableColumn<>("Товары");
        itemsCol.setCellValueFactory(data -> data.getValue().itemsProperty());
        itemsCol.setPrefWidth(250);

        table.getColumns().addAll(idCol, dateCol, supplierCol, receiverCol, amountCol, itemsCol);

        exportWordBtn.setOnAction(e -> {
            SupplyFX selected = table.getSelectionModel().getSelectedItem();
            if (selected == null) {
                showAlert("Ошибка", "Выберите строку для экспорта");
                return;
            }
            exportSupplyToDocx(selected);
        });

        List<SupplyFX> allSupplies = new ArrayList<>();

        Runnable applySupplyFilter = () -> {
            String search = searchField.getText().trim().toLowerCase();
            table.getItems().clear();
            for (SupplyFX s : allSupplies) {
                // Фильтр по дате
                if (fromDate.getValue() != null && toDate.getValue() != null) {
                    String suppDate = s.getDate() != null && s.getDate().length() >= 10
                            ? s.getDate().substring(0, 10) : "";
                    if (suppDate.compareTo(fromDate.getValue().toString()) < 0 ||
                            suppDate.compareTo(toDate.getValue().toString()) > 0) continue;
                }
                if (!sumFromField.getText().trim().isEmpty()) {
                    try {
                        double minSum = Double.parseDouble(sumFromField.getText().trim().replace(",", "."));
                        if (s.getTotalAmount() < minSum) continue;
                    } catch (NumberFormatException ignored) {}
                }
                if (!sumToField.getText().trim().isEmpty()) {
                    try {
                        double maxSum = Double.parseDouble(sumToField.getText().trim().replace(",", "."));
                        if (s.getTotalAmount() > maxSum) continue;
                    } catch (NumberFormatException ignored) {}
                }
                if (!search.isEmpty()) {
                    if (!s.getSupplier().toLowerCase().contains(search) &&
                            !s.getReceiver().toLowerCase().contains(search) &&
                            !s.getItems().toLowerCase().contains(search)) continue;
                }
                table.getItems().add(s);
            }
        };

        searchField.textProperty().addListener((obs, oldVal, newVal) -> applySupplyFilter.run());

        filterDateBtn.setOnAction(e -> applySupplyFilter.run());

        resetBtn.setOnAction(e -> {
            searchField.clear();
            fromDate.setValue(LocalDate.now().minusDays(30));
            toDate.setValue(LocalDate.now());
            sumFromField.clear();
            sumToField.clear();
            applySupplyFilter.run();
        });

        Runnable loadSupplies = () -> {
            allSupplies.clear();
            try {
                out.write("SUPPLIES_HISTORY");
                out.newLine(); out.flush();

                String line;
                while ((line = in.readLine()) != null) {
                    if (line.equals("===END===")) break;
                    try {
                        if (line.startsWith("[Поставка #")) {
                            int idStart = line.indexOf("#") + 1;
                            int idEnd = line.indexOf("]");
                            int id = Integer.parseInt(line.substring(idStart, idEnd));

                            String afterId = line.substring(idEnd + 2);
                            String[] parts = afterId.split(" \\| ");

                            String date = parts[0].trim();
                            String supplier = parts.length > 1 ? parts[1].replace("Поставщик: ", "").trim() : "";
                            String receiver = parts.length > 2 ? parts[2].replace("Принял: ", "").trim() : "";

                            double amount = 0;
                            String items = "";
                            if (parts.length > 3) {
                                String amountPart = parts[3].replace("Сумма: ", "").replace(" руб.", "").replace(",", ".").trim();
                                amount = Double.parseDouble(amountPart);
                            }
                            if (parts.length > 4) {
                                items = parts[4].replace("Товары: ", "").trim();
                            }

                            allSupplies.add(new SupplyFX(id, date, supplier, receiver, amount, items));
                        }
                    } catch (Exception ex) {
                        System.err.println("Ошибка парсинга поставки: " + line);
                    }
                }
                applySupplyFilter.run();
            } catch (IOException ex) {
                System.err.println("Ошибка загрузки истории поставок");
            }
        };

        Button refreshBtn = new Button("Обновить");
        refreshBtn.setStyle("-fx-background-color: #6b4c3b; -fx-text-fill: white; -fx-cursor: hand;");
        refreshBtn.setOnAction(e -> loadSupplies.run());

        loadSupplies.run();

        vbox.getChildren().addAll(header, filterBox, table, refreshBtn);
        return vbox;
    }

    private void exportSupplyToDocx(SupplyFX supply) {
        FileChooser fileChooser = new FileChooser();
        fileChooser.setTitle("Сохранить документ о поставке");
        fileChooser.getExtensionFilters().add(new FileChooser.ExtensionFilter("Word Document", "*.doc"));
        fileChooser.setInitialFileName("Поставка_№" + supply.getId() + ".doc");

        File file = fileChooser.showSaveDialog(null);
        if (file != null) {
            try {
                WordExporter.exportSupplyToWord(supply, currentFullName, file);
                showAlert("Экспорт", "Документ сохранён!\n" + file.getName() + "\nОткройте в Microsoft Word.");
            } catch (IOException ex) {
                showAlert("Ошибка", "Не удалось сохранить файл");
            }
        }
    }

    private void loadCategories(ComboBox<String> combo) {
        try {
            out.write("CATEGORIES_LIST");
            out.newLine();
            out.flush();

            String line;
            while ((line = in.readLine()) != null) {
                if (line.equals("===END===")) break;
                if (!line.trim().isEmpty()) {
                    String[] parts = line.split("\\|");
                    if (parts.length == 2) {
                        combo.getItems().add(parts[1] + " [" + parts[0] + "]");
                    }
                }
            }
        } catch (IOException ex) {
            System.err.println("Ошибка загрузки категорий: " + ex.getMessage());
        }
    }

    private int getSelectedCategoryId(ComboBox<String> combo) {
        String selected = combo.getSelectionModel().getSelectedItem();
        if (selected == null || selected.equals("Все категории")) return 0;
        int start = selected.lastIndexOf("[");
        int end = selected.lastIndexOf("]");
        if (start >= 0 && end > start) {
            return Integer.parseInt(selected.substring(start + 1, end));
        }
        return 0;
    }

    private void loadFilteredProducts(TableView<ProductFX> table, int categoryId, String search) {
        table.getItems().clear();
        try {
            String cmd = "FILTER_PRODUCTS:" + categoryId + "|" + (search.isEmpty() ? "" : search);
            out.write(cmd);
            out.newLine();
            out.flush();
            parseProductsFromServer(table);
        } catch (IOException ex) {
            System.err.println("Ошибка фильтрации: " + ex.getMessage());
        }
    }

    private void parseProductsFromServer(TableView<ProductFX> table) {
        try {
            String line;
            while ((line = in.readLine()) != null) {
                if (line.equals("===END===")) break;
                try {
                    if (line.startsWith("===") || line.startsWith("Всего")) continue;

                    String s = line;
                    if (s.startsWith("[") && s.contains("]")) {
                        int idEnd = s.indexOf("]");
                        int id = Integer.parseInt(s.substring(1, idEnd));

                        String data = s.substring(idEnd + 2);
                        String[] parts = data.split("\\|");

                        if (parts.length >= 8) {
                            String name = parts[0];
                            String brand = parts[1].equals("-") ? "" : parts[1];
                            int volume = Integer.parseInt(parts[2]);
                            String cat = parts[3];
                            double purchPrice = Double.parseDouble(parts[4].replace(",", "."));
                            double retailPrice = Double.parseDouble(parts[5].replace(",", "."));
                            int stock = Integer.parseInt(parts[6]);
                            String desc = parts[7];

                            table.getItems().add(new ProductFX(id, name, brand, cat, volume,
                                    purchPrice, retailPrice, stock, desc));
                        }
                    }
                } catch (Exception ex) {
                    System.err.println("Ошибка парсинга строки: " + line + " - " + ex.getMessage());
                }
            }
        } catch (IOException ex) {
            System.err.println("Ошибка чтения: " + ex.getMessage());
        }
    }

    private void showAddProductDialog(TableView<ProductFX> table) {
        Dialog<ButtonType> dialog = new Dialog<>();
        dialog.setTitle("Добавить товар");
        dialog.setHeaderText("Заполните данные нового товара");

        ButtonType saveBtnType = new ButtonType("Сохранить", ButtonBar.ButtonData.OK_DONE);
        dialog.getDialogPane().getButtonTypes().addAll(saveBtnType, ButtonType.CANCEL);

        GridPane grid = new GridPane();
        grid.setHgap(10);
        grid.setVgap(10);
        grid.setPadding(new Insets(20));

        TextField nameField = new TextField();
        nameField.setPromptText("Название");

        ComboBox<String> catCombo = new ComboBox<>();
        catCombo.setPromptText("Категория");
        catCombo.setPrefWidth(250);
        loadCategories(catCombo);

        TextField brandField = new TextField();
        brandField.setPromptText("Бренд");

        TextField volumeField = new TextField();
        volumeField.setPromptText("Объём (мл)");

        TextField purchPriceField = new TextField();
        purchPriceField.setPromptText("Закупочная цена");

        TextField retailPriceField = new TextField();
        retailPriceField.setPromptText("Розничная цена");

        TextField stockField = new TextField();
        stockField.setPromptText("Количество");

        TextField descField = new TextField();
        descField.setPromptText("Описание");

        grid.add(new Label("Название:"), 0, 0);
        grid.add(nameField, 1, 0);
        grid.add(new Label("Категория:"), 0, 1);
        grid.add(catCombo, 1, 1);
        grid.add(new Label("Бренд:"), 0, 2);
        grid.add(brandField, 1, 2);
        grid.add(new Label("Объём (мл):"), 0, 3);
        grid.add(volumeField, 1, 3);
        grid.add(new Label("Закуп. цена:"), 0, 4);
        grid.add(purchPriceField, 1, 4);
        grid.add(new Label("Розн. цена:"), 0, 5);
        grid.add(retailPriceField, 1, 5);
        grid.add(new Label("Остаток:"), 0, 6);
        grid.add(stockField, 1, 6);
        grid.add(new Label("Описание:"), 0, 7);
        grid.add(descField, 1, 7);

        dialog.getDialogPane().setContent(grid);

        final Button saveButton = (Button) dialog.getDialogPane().lookupButton(saveBtnType);
        saveButton.addEventFilter(javafx.event.ActionEvent.ACTION, event -> {
            // Валидация
            if (nameField.getText().trim().isEmpty()) {
                showAlert("Ошибка", "Введите название товара");
                event.consume();
                return;
            }
            int catId = getSelectedCategoryId(catCombo);
            if (catId == 0) {
                showAlert("Ошибка", "Выберите категорию");
                event.consume();
                return;
            }
            try {
                String volStr = volumeField.getText().trim();
                if (!volStr.isEmpty()) Integer.parseInt(volStr);
                String purchStr = purchPriceField.getText().trim().replace(",", ".");
                if (!purchStr.isEmpty()) Double.parseDouble(purchStr);
                String retailStr = retailPriceField.getText().trim().replace(",", ".");
                if (!retailStr.isEmpty()) Double.parseDouble(retailStr);
                String stockStr = stockField.getText().trim();
                if (!stockStr.isEmpty()) Integer.parseInt(stockStr);
            } catch (NumberFormatException ex) {
                showAlert("Ошибка", "Проверьте числовые поля: объём, цены, количество");
                event.consume();
                return;
            }

            try {
                String cmd = "ADD_PRODUCT:" +
                        nameField.getText().trim() + "|" + catId + "|" +
                        brandField.getText().trim() + "|" +
                        (volumeField.getText().trim().isEmpty() ? "0" : volumeField.getText().trim()) + "|" +
                        (purchPriceField.getText().trim().isEmpty() ? "0" : purchPriceField.getText().trim().replace(",", ".")) + "|" +
                        (retailPriceField.getText().trim().isEmpty() ? "0" : retailPriceField.getText().trim().replace(",", ".")) + "|" +
                        (stockField.getText().trim().isEmpty() ? "0" : stockField.getText().trim()) + "|" +
                        descField.getText().trim();

                out.write(cmd); out.newLine(); out.flush();
                String response = in.readLine(); in.readLine();
                if (response.contains("добавлен")) {
                    showAlert("Успех", "Товар успешно добавлен");
                    table.getItems().clear();
                    loadProducts(table);
                } else {
                    showAlert("Ошибка", response);
                }
            } catch (Exception e) {
                showAlert("Ошибка", e.getMessage());
                event.consume();
            }
        });

        dialog.showAndWait();
    }

    private void showEditProductDialog(TableView<ProductFX> table) {
        ProductFX selected = table.getSelectionModel().getSelectedItem();
        if (selected == null) {
            showAlert("Ошибка", "Выберите товар для редактирования");
            return;
        }

        final String[] fullData;
        try {
            out.write("GET_PRODUCT:" + selected.getId());
            out.newLine();
            out.flush();

            String response = in.readLine();
            in.readLine();

            if (response == null || response.startsWith("Товар не найден")) {
                showAlert("Ошибка", "Не удалось загрузить данные товара");
                return;
            }
            fullData = response.split("\\|");
        } catch (IOException e) {
            showAlert("Ошибка", "Не удалось получить данные товара");
            return;
        }

        if (fullData.length < 10) {
            showAlert("Ошибка", "Неполные данные товара");
            return;
        }

        Dialog<ButtonType> dialog = new Dialog<>();
        dialog.setTitle("Редактировать товар");
        dialog.setHeaderText("Редактирование: " + fullData[1]);

        ButtonType saveBtnType = new ButtonType("Сохранить", ButtonBar.ButtonData.OK_DONE);
        dialog.getDialogPane().getButtonTypes().addAll(saveBtnType, ButtonType.CANCEL);

        GridPane grid = new GridPane();
        grid.setHgap(10);
        grid.setVgap(10);
        grid.setPadding(new Insets(20));

        TextField nameField = new TextField(fullData[1]);

        ComboBox<String> catCombo = new ComboBox<>();
        catCombo.setPrefWidth(250);
        loadCategories(catCombo);
        for (String item : catCombo.getItems()) {
            if (item.contains(fullData[3])) {
                catCombo.getSelectionModel().select(item);
                break;
            }
        }

        TextField brandField = new TextField(fullData[4]);
        TextField volumeField = new TextField(fullData[5]);
        TextField purchPriceField = new TextField(fullData[6]);
        TextField retailPriceField = new TextField(fullData[7]);
        TextField stockField = new TextField(fullData[8]);
        TextField descField = new TextField(fullData[9]);

        grid.add(new Label("Название:"), 0, 0);
        grid.add(nameField, 1, 0);
        grid.add(new Label("Категория:"), 0, 1);
        grid.add(catCombo, 1, 1);
        grid.add(new Label("Бренд:"), 0, 2);
        grid.add(brandField, 1, 2);
        grid.add(new Label("Объём (мл):"), 0, 3);
        grid.add(volumeField, 1, 3);
        grid.add(new Label("Закуп. цена:"), 0, 4);
        grid.add(purchPriceField, 1, 4);
        grid.add(new Label("Розн. цена:"), 0, 5);
        grid.add(retailPriceField, 1, 5);
        grid.add(new Label("Остаток:"), 0, 6);
        grid.add(stockField, 1, 6);
        grid.add(new Label("Описание:"), 0, 7);
        grid.add(descField, 1, 7);

        dialog.getDialogPane().setContent(grid);

        final Button saveButton = (Button) dialog.getDialogPane().lookupButton(saveBtnType);
        saveButton.addEventFilter(javafx.event.ActionEvent.ACTION, event -> {
            if (nameField.getText().trim().isEmpty()) {
                showAlert("Ошибка", "Введите название товара");
                event.consume();
                return;
            }
            int catId = getSelectedCategoryId(catCombo);
            if (catId == 0) {
                showAlert("Ошибка", "Выберите категорию");
                event.consume();
                return;
            }
            try {
                String volStr = volumeField.getText().trim();
                if (!volStr.isEmpty()) Integer.parseInt(volStr);
                String purchStr = purchPriceField.getText().trim().replace(",", ".");
                if (!purchStr.isEmpty()) Double.parseDouble(purchStr);
                String retailStr = retailPriceField.getText().trim().replace(",", ".");
                if (!retailStr.isEmpty()) Double.parseDouble(retailStr);
                String stockStr = stockField.getText().trim();
                if (!stockStr.isEmpty()) Integer.parseInt(stockStr);
            } catch (NumberFormatException ex) {
                showAlert("Ошибка", "Проверьте числовые поля: объём, цены, количество");
                event.consume();
                return;
            }

            try {
                String cmd = "UPDATE_PRODUCT:" + fullData[0] + "|" +
                        nameField.getText().trim() + "|" + catId + "|" +
                        brandField.getText().trim() + "|" +
                        volumeField.getText().trim() + "|" +
                        (purchPriceField.getText().trim().isEmpty() ? "0" : purchPriceField.getText().trim().replace(",", ".")) + "|" +
                        (retailPriceField.getText().trim().isEmpty() ? "0" : retailPriceField.getText().trim().replace(",", ".")) + "|" +
                        stockField.getText().trim() + "|" +
                        descField.getText().trim();

                out.write(cmd); out.newLine(); out.flush();
                String response = in.readLine(); in.readLine();

                if (response.contains("обновлён")) {
                    showAlert("Успех", "Товар обновлён");
                    table.getItems().clear();
                    loadProducts(table);
                } else {
                    showAlert("Ошибка", response);
                    event.consume();
                }
            } catch (Exception e) {
                showAlert("Ошибка", e.getMessage());
                event.consume();
            }
        });

        dialog.showAndWait();
    }

    private void deactivateProduct(TableView<ProductFX> table) {
        ProductFX selected = table.getSelectionModel().getSelectedItem();
        if (selected == null) {
            showAlert("Ошибка", "Выберите товар для удаления");
            return;
        }

        Alert confirm = new Alert(Alert.AlertType.CONFIRMATION);
        confirm.setTitle("Подтверждение");
        confirm.setHeaderText("Удалить товар?");
        confirm.setContentText("Товар \"" + selected.getName() + "\" будет удален из списка.");

        confirm.showAndWait().ifPresent(result -> {
            if (result == ButtonType.OK) {
                try {
                    out.write("DELETE_PRODUCT:" + selected.getId());
                    out.newLine();
                    out.flush();

                    in.readLine();
                    in.readLine();

                    showAlert("Успех", "Товар удален");
                    table.getItems().clear();
                    loadProducts(table);
                } catch (IOException e) {
                    showAlert("Ошибка", e.getMessage());
                }
            }
        });
    }

    private Tab createClientsTab(String role) {
        Tab tab = new Tab("Клиенты");

        VBox vbox = new VBox(10);
        vbox.setPadding(new Insets(10));
        vbox.setStyle("-fx-background-color: #ffffff;");

        Text header = new Text("Список клиентов");
        header.setFont(Font.font("System", FontWeight.BOLD, 16));
        header.setFill(Color.rgb(80, 50, 30));

        HBox searchBox = new HBox(10);
        searchBox.setAlignment(Pos.CENTER_LEFT);

        TextField searchField = new TextField();
        searchField.setPromptText("Поиск");
        searchField.setPrefWidth(300);

        Button resetBtn = new Button("Сбросить");
        resetBtn.setStyle("-fx-background-color: #999; -fx-text-fill: white; -fx-cursor: hand;");

        searchBox.getChildren().addAll(new Label("Поиск:"), searchField, resetBtn);

        TableView<ClientFX> table = new TableView<>();
        table.setPrefHeight(380);
        table.setStyle("-fx-font-family: 'System'; -fx-font-size: 12px;");

        TableColumn<ClientFX, Number> idCol = new TableColumn<>("ID");
        idCol.setCellValueFactory(data -> data.getValue().idProperty());
        idCol.setPrefWidth(40);

        TableColumn<ClientFX, String> nameCol = new TableColumn<>("ФИО");
        nameCol.setCellValueFactory(data -> data.getValue().nameProperty());
        nameCol.setPrefWidth(180);

        TableColumn<ClientFX, String> phoneCol = new TableColumn<>("Телефон");
        phoneCol.setCellValueFactory(data -> data.getValue().phoneProperty());
        phoneCol.setPrefWidth(130);

        TableColumn<ClientFX, String> emailCol = new TableColumn<>("Email");
        emailCol.setCellValueFactory(data -> data.getValue().emailProperty());
        emailCol.setPrefWidth(150);

        TableColumn<ClientFX, String> addressCol = new TableColumn<>("Адрес");
        addressCol.setCellValueFactory(data -> data.getValue().addressProperty());
        addressCol.setPrefWidth(200);

        table.getColumns().addAll(idCol, nameCol, phoneCol, emailCol, addressCol);

        final List<ClientFX> allClients = new ArrayList<>();

        searchField.textProperty().addListener((obs, oldVal, newVal) -> {
            String filter = newVal.trim().toLowerCase();
            table.getItems().clear();
            if (filter.isEmpty()) {
                table.getItems().addAll(allClients);
            } else {
                for (ClientFX c : allClients) {
                    if ((c.getName() != null && c.getName().toLowerCase().contains(filter)) ||
                            (c.getPhone() != null && c.getPhone().toLowerCase().contains(filter)) ||
                            (c.getEmail() != null && c.getEmail().toLowerCase().contains(filter)) ||
                            (c.getAddress() != null && c.getAddress().toLowerCase().contains(filter)) ||
                            String.valueOf(c.getId()).contains(filter)) {
                        table.getItems().add(c);
                    }
                }
            }
        });

        resetBtn.setOnAction(e -> {
            searchField.clear();
            table.getItems().clear();
            table.getItems().addAll(allClients);
        });

        HBox btnBox = new HBox(10);

        Button refreshBtn = new Button("Обновить");
        refreshBtn.setStyle("-fx-background-color: #6b4c3b; -fx-text-fill: white; -fx-cursor: hand;");
        refreshBtn.setOnAction(e -> {
            allClients.clear();
            loadClientsList(allClients, table);
        });

        Button addBtn = new Button("Добавить");
        addBtn.setStyle("-fx-background-color: #4a7c3f; -fx-text-fill: white; -fx-cursor: hand;");
        addBtn.setOnAction(e -> {
            showAddClientDialog(table);
            allClients.clear();
            loadClientsList(allClients, table);
        });

        Button editBtn = new Button("Редактировать");
        editBtn.setStyle("-fx-background-color: #c49b2a; -fx-text-fill: white; -fx-cursor: hand;");
        editBtn.setOnAction(e -> {
            showEditClientDialog(table);
            allClients.clear();
            loadClientsList(allClients, table);
        });

        Button deleteBtn = new Button("Удалить");
        deleteBtn.setStyle("-fx-background-color: #b53b3b; -fx-text-fill: white; -fx-cursor: hand;");
        deleteBtn.setOnAction(e -> {
            deactivateClient(table);
            allClients.clear();
            loadClientsList(allClients, table);
        });

        if (!role.equals("ADMIN")) deleteBtn.setVisible(false);

        btnBox.getChildren().addAll(refreshBtn, addBtn, editBtn, deleteBtn);

        loadClientsList(allClients, table);

        vbox.getChildren().addAll(header, searchBox, table, btnBox);
        tab.setContent(vbox);
        return tab;
    }

    private void loadClientsList(List<ClientFX> allClients, TableView<ClientFX> table) {
        allClients.clear();
        table.getItems().clear();
        try {
            out.write("CLIENTS");
            out.newLine(); out.flush();

            String line;
            while ((line = in.readLine()) != null) {
                if (line.equals("===END===")) break;
                if (line.trim().isEmpty()) continue;
                String[] parts = line.split("\\|");
                if (parts.length >= 5) {
                    ClientFX c = new ClientFX(
                            Integer.parseInt(parts[0]), parts[1], parts[2], parts[3], parts[4]);
                    allClients.add(c);
                }
            }
            table.getItems().addAll(allClients);
        } catch (IOException ex) {
            System.err.println("Ошибка загрузки клиентов");
        }
    }

    private Tab createSuppliersTabPage(String role) {
        Tab tab = new Tab("Поставщики");

        VBox vbox = new VBox(10);
        vbox.setPadding(new Insets(10));
        vbox.setStyle("-fx-background-color: #ffffff;");

        Text header = new Text("Список поставщиков");
        header.setFont(Font.font("System", FontWeight.BOLD, 16));
        header.setFill(Color.rgb(80, 50, 30));

        HBox searchBox = new HBox(10);
        searchBox.setAlignment(Pos.CENTER_LEFT);

        TextField searchField = new TextField();
        searchField.setPromptText("Поиск");
        searchField.setPrefWidth(300);

        Button resetBtn = new Button("Сбросить");
        resetBtn.setStyle("-fx-background-color: #999; -fx-text-fill: white; -fx-cursor: hand;");

        searchBox.getChildren().addAll(new Label("Поиск:"), searchField, resetBtn);

        TableView<SupplierFX> table = new TableView<>();
        table.setPrefHeight(380);
        table.setStyle("-fx-font-family: 'System'; -fx-font-size: 12px;");

        TableColumn<SupplierFX, Number> idCol = new TableColumn<>("ID");
        idCol.setCellValueFactory(data -> data.getValue().idProperty());
        idCol.setPrefWidth(40);

        TableColumn<SupplierFX, String> nameCol = new TableColumn<>("Название");
        nameCol.setCellValueFactory(data -> data.getValue().nameProperty());
        nameCol.setPrefWidth(180);

        TableColumn<SupplierFX, String> contactCol = new TableColumn<>("Контакт");
        contactCol.setCellValueFactory(data -> data.getValue().contactPersonProperty());
        contactCol.setPrefWidth(130);

        TableColumn<SupplierFX, String> phoneCol = new TableColumn<>("Телефон");
        phoneCol.setCellValueFactory(data -> data.getValue().phoneProperty());
        phoneCol.setPrefWidth(120);

        TableColumn<SupplierFX, String> emailCol = new TableColumn<>("Email");
        emailCol.setCellValueFactory(data -> data.getValue().emailProperty());
        emailCol.setPrefWidth(150);

        TableColumn<SupplierFX, String> addressCol = new TableColumn<>("Адрес");
        addressCol.setCellValueFactory(data -> data.getValue().addressProperty());
        addressCol.setPrefWidth(200);

        table.getColumns().addAll(idCol, nameCol, contactCol, phoneCol, emailCol, addressCol);

        final List<SupplierFX> allSuppliers = new ArrayList<>();

        searchField.textProperty().addListener((obs, oldVal, newVal) -> {
            String filter = newVal.trim().toLowerCase();
            table.getItems().clear();
            if (filter.isEmpty()) {
                table.getItems().addAll(allSuppliers);
            } else {
                for (SupplierFX s : allSuppliers) {
                    if ((s.getName() != null && s.getName().toLowerCase().contains(filter)) ||
                            (s.getContactPerson() != null && s.getContactPerson().toLowerCase().contains(filter)) ||
                            (s.getPhone() != null && s.getPhone().toLowerCase().contains(filter)) ||
                            (s.getEmail() != null && s.getEmail().toLowerCase().contains(filter)) ||
                            (s.getAddress() != null && s.getAddress().toLowerCase().contains(filter)) ||
                            String.valueOf(s.getId()).contains(filter)) {
                        table.getItems().add(s);
                    }
                }
            }
        });

        resetBtn.setOnAction(e -> {
            searchField.clear();
            table.getItems().clear();
            table.getItems().addAll(allSuppliers);
        });

        HBox btnBox = new HBox(10);

        Button refreshBtn = new Button("Обновить");
        refreshBtn.setStyle("-fx-background-color: #6b4c3b; -fx-text-fill: white; -fx-cursor: hand;");
        refreshBtn.setOnAction(e -> {
            allSuppliers.clear();
            loadSuppliersList(allSuppliers, table);
        });

        Button addBtn = new Button("Добавить");
        addBtn.setStyle("-fx-background-color: #4a7c3f; -fx-text-fill: white; -fx-cursor: hand;");
        addBtn.setOnAction(e -> {
            showAddSupplierFullDialog(table);
            allSuppliers.clear();
            loadSuppliersList(allSuppliers, table);
        });

        Button editBtn = new Button("Редактировать");
        editBtn.setStyle("-fx-background-color: #c49b2a; -fx-text-fill: white; -fx-cursor: hand;");
        editBtn.setOnAction(e -> {
            showEditSupplierDialog(table);
            allSuppliers.clear();
            loadSuppliersList(allSuppliers, table);
        });

        Button deleteBtn = new Button("Удалить");
        deleteBtn.setStyle("-fx-background-color: #b53b3b; -fx-text-fill: white; -fx-cursor: hand;");
        deleteBtn.setOnAction(e -> {
            deactivateSupplier(table);
            allSuppliers.clear();
            loadSuppliersList(allSuppliers, table);
        });

        if (!role.equals("ADMIN")) deleteBtn.setVisible(false);

        btnBox.getChildren().addAll(refreshBtn, addBtn, editBtn, deleteBtn);

        loadSuppliersList(allSuppliers, table);

        vbox.getChildren().addAll(header, searchBox, table, btnBox);
        tab.setContent(vbox);
        return tab;
    }

    private void loadSuppliersList(List<SupplierFX> allSuppliers, TableView<SupplierFX> table) {
        allSuppliers.clear();
        table.getItems().clear();
        try {
            out.write("SUPPLIERS_LIST");
            out.newLine(); out.flush();

            String line;
            while ((line = in.readLine()) != null) {
                if (line.equals("===END===")) break;
                if (line.trim().isEmpty()) continue;
                String[] parts = line.split("\\|");
                if (parts.length >= 5) {
                    String address = parts.length >= 6 ? parts[5] : "";
                    SupplierFX s = new SupplierFX(
                            Integer.parseInt(parts[0]), parts[1],
                            parts.length > 2 ? parts[2] : "",
                            parts.length > 3 ? parts[3] : "",
                            parts.length > 4 ? parts[4] : "",
                            address);
                    allSuppliers.add(s);
                }
            }
            table.getItems().addAll(allSuppliers);
        } catch (IOException ex) {
            System.err.println("Ошибка загрузки поставщиков");
        }
    }

    private void showEditSupplierDialog(TableView<SupplierFX> table) {
        SupplierFX selected = table.getSelectionModel().getSelectedItem();
        if (selected == null) { showAlert("Ошибка", "Выберите поставщика"); return; }

        Dialog<ButtonType> dialog = new Dialog<>();
        dialog.setTitle("Редактировать поставщика");
        dialog.setHeaderText(selected.getName());

        ButtonType saveBtn = new ButtonType("Сохранить", ButtonBar.ButtonData.OK_DONE);
        dialog.getDialogPane().getButtonTypes().addAll(saveBtn, ButtonType.CANCEL);

        GridPane grid = new GridPane();
        grid.setHgap(10); grid.setVgap(10); grid.setPadding(new Insets(20));

        TextField nameField = new TextField(selected.getName());
        TextField contactField = new TextField(selected.getContactPerson());
        TextField phoneField = new TextField(selected.getPhone());
        TextField emailField = new TextField(selected.getEmail());
        TextField addressField = new TextField(selected.getAddress());

        grid.add(new Label("Название:"), 0, 0); grid.add(nameField, 1, 0);
        grid.add(new Label("Контакт:"), 0, 1); grid.add(contactField, 1, 1);
        grid.add(new Label("Телефон:"), 0, 2); grid.add(phoneField, 1, 2);
        grid.add(new Label("Email:"), 0, 3); grid.add(emailField, 1, 3);
        grid.add(new Label("Адрес:"), 0, 4); grid.add(addressField, 1, 4);

        dialog.getDialogPane().setContent(grid);

        dialog.showAndWait().ifPresent(bt -> {
            if (bt == saveBtn && !nameField.getText().trim().isEmpty()) {
                try {
                    out.write("UPDATE_SUPPLIER:" + selected.getId() + "|" + nameField.getText().trim() + "|" +
                            contactField.getText().trim() + "|" + phoneField.getText().trim() + "|" +
                            emailField.getText().trim() + "|" + addressField.getText().trim());
                    out.newLine(); out.flush();
                    in.readLine(); in.readLine();
                    String resp = in.readLine(); in.readLine();
                    if (resp.contains("обновлён")) showAlert("Успех", "Поставщик обновлён");
                    loadSuppliersTable(table);
                } catch (IOException ex) { showAlert("Ошибка", ex.getMessage()); }
            }
        });
    }

    private void loadClientsTable(TableView<ClientFX> table) {
        table.getItems().clear();
        try {
            out.write("CLIENTS");
            out.newLine(); out.flush();

            String line;
            while ((line = in.readLine()) != null) {
                if (line.equals("===END===")) break;
                String[] parts = line.split("\\|");
                if (parts.length >= 5) {
                    table.getItems().add(new ClientFX(
                            Integer.parseInt(parts[0]), parts[1], parts[2], parts[3], parts[4]));
                }
            }
        } catch (IOException ex) {
            System.err.println("Ошибка загрузки клиентов");
        }
    }

    private void showAddClientDialog(TableView<ClientFX> table) {
        Dialog<ButtonType> dialog = new Dialog<>();
        dialog.setTitle("Добавить клиента");
        dialog.setHeaderText("Новый клиент");

        ButtonType saveBtn = new ButtonType("Сохранить", ButtonBar.ButtonData.OK_DONE);
        dialog.getDialogPane().getButtonTypes().addAll(saveBtn, ButtonType.CANCEL);

        GridPane grid = new GridPane();
        grid.setHgap(10);
        grid.setVgap(10);
        grid.setPadding(new Insets(20));

        TextField nameField = new TextField();
        nameField.setPromptText("ФИО *");
        TextField phoneField = new TextField();
        phoneField.setPromptText("Телефон");
        TextField emailField = new TextField();
        emailField.setPromptText("Email");
        TextField addressField = new TextField();
        addressField.setPromptText("Адрес");

        grid.add(new Label("ФИО:"), 0, 0); grid.add(nameField, 1, 0);
        grid.add(new Label("Телефон:"), 0, 1); grid.add(phoneField, 1, 1);
        grid.add(new Label("Email:"), 0, 2); grid.add(emailField, 1, 2);
        grid.add(new Label("Адрес:"), 0, 3); grid.add(addressField, 1, 3);

        dialog.getDialogPane().setContent(grid);

        dialog.showAndWait().ifPresent(bt -> {
            if (bt == saveBtn && !nameField.getText().trim().isEmpty()) {
                try {
                    out.write("ADD_CLIENT:" + nameField.getText().trim() + "|" +
                            phoneField.getText().trim() + "|" + emailField.getText().trim() + "|" +
                            addressField.getText().trim());
                    out.newLine(); out.flush();
                    in.readLine(); in.readLine();
                    String resp = in.readLine();
                    in.readLine();
                    if (resp.contains("добавлен")) {
                        showAlert("Успех", "Клиент добавлен");
                        reloadAllClientCombos();
                    }
                    loadClientsTable(table);
                } catch (IOException ex) { showAlert("Ошибка", ex.getMessage()); }
            }
        });
    }

    private void showEditClientDialog(TableView<ClientFX> table) {
        ClientFX selected = table.getSelectionModel().getSelectedItem();
        if (selected == null) { showAlert("Ошибка", "Выберите клиента"); return; }

        Dialog<ButtonType> dialog = new Dialog<>();
        dialog.setTitle("Редактировать клиента");
        dialog.setHeaderText(selected.getName());

        ButtonType saveBtn = new ButtonType("Сохранить", ButtonBar.ButtonData.OK_DONE);
        dialog.getDialogPane().getButtonTypes().addAll(saveBtn, ButtonType.CANCEL);

        GridPane grid = new GridPane();
        grid.setHgap(10); grid.setVgap(10); grid.setPadding(new Insets(20));

        TextField nameField = new TextField(selected.getName());
        TextField phoneField = new TextField(selected.getPhone());
        TextField emailField = new TextField(selected.getEmail());
        TextField addressField = new TextField(selected.getAddress());

        grid.add(new Label("ФИО:"), 0, 0); grid.add(nameField, 1, 0);
        grid.add(new Label("Телефон:"), 0, 1); grid.add(phoneField, 1, 1);
        grid.add(new Label("Email:"), 0, 2); grid.add(emailField, 1, 2);
        grid.add(new Label("Адрес:"), 0, 3); grid.add(addressField, 1, 3);

        dialog.getDialogPane().setContent(grid);

        dialog.showAndWait().ifPresent(bt -> {
            if (bt == saveBtn) {
                try {
                    out.write("UPDATE_CLIENT:" + selected.getId() + "|" + nameField.getText().trim() + "|" +
                            phoneField.getText().trim() + "|" + emailField.getText().trim() + "|" + addressField.getText().trim());
                    out.newLine(); out.flush();
                    in.readLine(); in.readLine();
                    String resp = in.readLine(); in.readLine();
                    if (resp.contains("обновлён")) {
                        showAlert("Успех", "Клиент обновлён");
                        reloadAllClientCombos();
                    }
                    loadClientsTable(table);
                } catch (IOException ex) { showAlert("Ошибка", ex.getMessage()); }
            }
        });
    }

    private void deactivateClient(TableView<ClientFX> table) {
        ClientFX selected = table.getSelectionModel().getSelectedItem();
        if (selected == null) { showAlert("Ошибка", "Выберите клиента"); return; }

        Alert confirm = new Alert(Alert.AlertType.CONFIRMATION);
        confirm.setTitle("Подтверждение");
        confirm.setHeaderText("Удалить клиента?");
        confirm.showAndWait().ifPresent(r -> {
            if (r == ButtonType.OK) {
                try {
                    out.write("DELETE_CLIENT:" + selected.getId());
                    out.newLine(); out.flush();
                    in.readLine(); in.readLine();
                    loadClientsTable(table);
                    reloadAllClientCombos();
                } catch (IOException ex) { showAlert("Ошибка", ex.getMessage()); }
            }
        });
    }

    private void showAddClientFromSaleDialog(ComboBox<String> combo) {
        Dialog<ButtonType> dialog = new Dialog<>();
        dialog.setTitle("Добавить клиента");
        dialog.setHeaderText("Новый клиент");

        ButtonType saveBtn = new ButtonType("Сохранить", ButtonBar.ButtonData.OK_DONE);
        dialog.getDialogPane().getButtonTypes().addAll(saveBtn, ButtonType.CANCEL);

        GridPane grid = new GridPane();
        grid.setHgap(10); grid.setVgap(10); grid.setPadding(new Insets(20));

        TextField nameField = new TextField(); nameField.setPromptText("ФИО *");
        TextField phoneField = new TextField(); phoneField.setPromptText("Телефон");
        TextField emailField = new TextField(); emailField.setPromptText("Email");
        TextField addressField = new TextField(); addressField.setPromptText("Адрес");

        grid.add(new Label("ФИО:"), 0, 0); grid.add(nameField, 1, 0);
        grid.add(new Label("Телефон:"), 0, 1); grid.add(phoneField, 1, 1);
        grid.add(new Label("Email:"), 0, 2); grid.add(emailField, 1, 2);
        grid.add(new Label("Адрес:"), 0, 3); grid.add(addressField, 1, 3);

        dialog.getDialogPane().setContent(grid);

        dialog.showAndWait().ifPresent(bt -> {
            if (bt == saveBtn && !nameField.getText().trim().isEmpty()) {
                try {
                    out.write("ADD_CLIENT:" + nameField.getText().trim() + "|" +
                            phoneField.getText().trim() + "|" + emailField.getText().trim() + "|" +
                            addressField.getText().trim());
                    out.newLine(); out.flush();
                    String resp = in.readLine();
                    in.readLine();

                    if (resp.contains("добавлен")) {
                        showAlert("Успех", "Клиент добавлен");
                        reloadAllClientCombos();
                        combo.getItems().clear();
                        loadClients(combo);
                        combo.getEditor().setText(nameField.getText().trim());
                    } else {
                        showAlert("Ошибка", resp);
                    }
                } catch (IOException ex) { showAlert("Ошибка", ex.getMessage()); }
            }
        });
    }

    private void loadSuppliersTable(TableView<SupplierFX> table) {
        table.getItems().clear();
        try {
            out.write("SUPPLIERS_LIST");
            out.newLine(); out.flush();

            String line;
            while ((line = in.readLine()) != null) {
                if (line.equals("===END===")) break;
                if (line.trim().isEmpty()) continue;

                String[] parts = line.split("\\|");
                if (parts.length >= 5) {
                    String address = parts.length >= 6 ? parts[5] : "";
                    table.getItems().add(new SupplierFX(
                            Integer.parseInt(parts[0]),
                            parts[1],
                            parts.length > 2 ? parts[2] : "",
                            parts.length > 3 ? parts[3] : "",
                            parts.length > 4 ? parts[4] : "",
                            address));
                }
            }
        } catch (IOException ex) {
            System.err.println("Ошибка загрузки поставщиков: " + ex.getMessage());
        }
    }

    private void showAddSupplierFullDialog(TableView<SupplierFX> table) {
        Dialog<ButtonType> dialog = new Dialog<>();
        dialog.setTitle("Добавить поставщика");
        dialog.setHeaderText("Новый поставщик");

        ButtonType saveBtn = new ButtonType("Сохранить", ButtonBar.ButtonData.OK_DONE);
        dialog.getDialogPane().getButtonTypes().addAll(saveBtn, ButtonType.CANCEL);

        GridPane grid = new GridPane();
        grid.setHgap(10); grid.setVgap(10); grid.setPadding(new Insets(20));

        TextField nameField = new TextField(); nameField.setPromptText("Название *");
        TextField contactField = new TextField(); contactField.setPromptText("Контактное лицо");
        TextField phoneField = new TextField(); phoneField.setPromptText("Телефон");
        TextField emailField = new TextField(); emailField.setPromptText("Email");
        TextField addressField = new TextField(); addressField.setPromptText("Адрес");

        grid.add(new Label("Название:"), 0, 0); grid.add(nameField, 1, 0);
        grid.add(new Label("Контакт:"), 0, 1); grid.add(contactField, 1, 1);
        grid.add(new Label("Телефон:"), 0, 2); grid.add(phoneField, 1, 2);
        grid.add(new Label("Email:"), 0, 3); grid.add(emailField, 1, 3);
        grid.add(new Label("Адрес:"), 0, 4); grid.add(addressField, 1, 4);

        dialog.getDialogPane().setContent(grid);

        dialog.showAndWait().ifPresent(bt -> {
            if (bt == saveBtn && !nameField.getText().trim().isEmpty()) {
                try {
                    out.write("ADD_SUPPLIER:" + nameField.getText().trim() + "|" +
                            contactField.getText().trim() + "|" + phoneField.getText().trim() + "|" +
                            emailField.getText().trim() + "|" + addressField.getText().trim());
                    out.newLine(); out.flush();
                    in.readLine(); in.readLine();
                    String resp = in.readLine(); in.readLine();
                    if (resp.contains("добавлен")) {
                        showAlert("Успех", "Поставщик добавлен");
                        reloadAllSupplierCombos();
                    }
                    loadSuppliersTable(table);
                } catch (IOException ex) { showAlert("Ошибка", ex.getMessage()); }
            }
        });
    }

    private void deactivateSupplier(TableView<SupplierFX> table) {
        SupplierFX selected = table.getSelectionModel().getSelectedItem();
        if (selected == null) { showAlert("Ошибка", "Выберите поставщика"); return; }

        Alert confirm = new Alert(Alert.AlertType.CONFIRMATION);
        confirm.setTitle("Подтверждение");
        confirm.setHeaderText("Удалить поставщика?");
        confirm.showAndWait().ifPresent(r -> {
            if (r == ButtonType.OK) {
                try {
                    out.write("DELETE_SUPPLIER:" + selected.getId());
                    out.newLine(); out.flush();
                    in.readLine(); in.readLine();
                    loadSuppliersTable(table);
                    reloadAllSupplierCombos();
                } catch (IOException ex) { showAlert("Ошибка", ex.getMessage()); }
            }
        });
    }

    private void showAlert(String title, String message) {
        Alert alert = new Alert(Alert.AlertType.INFORMATION);
        alert.setTitle(title);
        alert.setHeaderText(null);
        alert.setContentText(message);
        alert.showAndWait();
    }

    private void loadProducts(TableView<ProductFX> table) {
        table.getItems().clear();
        try {
            out.write("LIST");
            out.newLine();
            out.flush();
            parseProductsFromServer(table);
        } catch (IOException ex) {
            System.err.println("Ошибка загрузки товаров: " + ex.getMessage());
        }
    }

    private void loadSuppliers(ComboBox<String> combo) {
        try {
            out.write("SUPPLIERS_LIST");
            out.newLine(); out.flush();

            String line;
            while ((line = in.readLine()) != null) {
                if (line.equals("===END===")) break;
                String[] parts = line.split("\\|");
                if (parts.length >= 2) {
                    combo.getItems().add(parts[1] + " (" + (parts.length > 2 ? parts[2] : "") + ") [" + parts[0] + "]");
                }
            }
        } catch (IOException ex) {
            System.err.println("Ошибка загрузки поставщиков");
        }
    }

    private void showAddSupplierDialog(ComboBox<String> combo) {
        Dialog<ButtonType> dialog = new Dialog<>();
        dialog.setTitle("Добавить поставщика");
        dialog.setHeaderText("Новый поставщик");

        ButtonType saveBtn = new ButtonType("Сохранить", ButtonBar.ButtonData.OK_DONE);
        dialog.getDialogPane().getButtonTypes().addAll(saveBtn, ButtonType.CANCEL);

        GridPane grid = new GridPane();
        grid.setHgap(10);
        grid.setVgap(10);
        grid.setPadding(new Insets(20));

        TextField nameField = new TextField();
        nameField.setPromptText("Название *");
        TextField contactField = new TextField();
        contactField.setPromptText("Контактное лицо");
        TextField phoneField = new TextField();
        phoneField.setPromptText("Телефон");
        TextField emailField = new TextField();
        emailField.setPromptText("Email");
        TextField addressField = new TextField();
        addressField.setPromptText("Адрес");

        grid.add(new Label("Название:"), 0, 0);
        grid.add(nameField, 1, 0);
        grid.add(new Label("Контакт:"), 0, 1);
        grid.add(contactField, 1, 1);
        grid.add(new Label("Телефон:"), 0, 2);
        grid.add(phoneField, 1, 2);
        grid.add(new Label("Email:"), 0, 3);
        grid.add(emailField, 1, 3);
        grid.add(new Label("Адрес:"), 0, 4);
        grid.add(addressField, 1, 4);

        dialog.getDialogPane().setContent(grid);

        dialog.showAndWait().ifPresent(buttonType -> {
            if (buttonType == saveBtn) {
                if (nameField.getText().trim().isEmpty()) {
                    showAlert("Ошибка", "Введите название");
                    return;
                }
                try {
                    out.write("ADD_SUPPLIER:" + nameField.getText().trim() + "|" +
                            contactField.getText().trim() + "|" + phoneField.getText().trim() + "|" +
                            emailField.getText().trim() + "|" + addressField.getText().trim());
                    out.newLine();
                    out.flush();
                    in.readLine();
                    in.readLine();
                    String resp = in.readLine(); in.readLine();
                    if (resp.contains("добавлен")) {
                        showAlert("Успех", "Поставщик добавлен");
                        reloadAllSupplierCombos();
                    }
                    combo.getItems().clear();
                    loadSuppliers(combo);
                    combo.getEditor().setText(nameField.getText().trim());
                } catch (IOException ex) {
                    showAlert("Ошибка", "Не удалось добавить поставщика");
                }
            }
        });
    }

    private Tab createUsersTab() {
        Tab tab = new Tab("Пользователи");

        VBox vbox = new VBox(10);
        vbox.setPadding(new Insets(10));
        vbox.setStyle("-fx-background-color: #ffffff;");

        Text header = new Text("Управление пользователями");
        header.setFont(Font.font("System", FontWeight.BOLD, 16));
        header.setFill(Color.rgb(80, 50, 30));

        HBox searchBox = new HBox(10);
        searchBox.setAlignment(Pos.CENTER_LEFT);

        TextField searchField = new TextField();
        searchField.setPromptText("Поиск по всем колонкам");
        searchField.setPrefWidth(300);

        Button resetBtn = new Button("Сбросить");
        resetBtn.setStyle("-fx-background-color: #999; -fx-text-fill: white; -fx-cursor: hand;");

        searchBox.getChildren().addAll(new Label("Поиск:"), searchField, resetBtn);

        TableView<UserFX> table = new TableView<>();
        table.setPrefHeight(380);

        TableColumn<UserFX, Number> idCol = new TableColumn<>("ID");
        idCol.setCellValueFactory(data -> data.getValue().idProperty());
        idCol.setPrefWidth(40);

        TableColumn<UserFX, String> loginCol = new TableColumn<>("Логин");
        loginCol.setCellValueFactory(data -> data.getValue().loginProperty());
        loginCol.setPrefWidth(120);

        TableColumn<UserFX, String> nameCol = new TableColumn<>("ФИО");
        nameCol.setCellValueFactory(data -> data.getValue().fullNameProperty());
        nameCol.setPrefWidth(200);

        TableColumn<UserFX, String> roleCol = new TableColumn<>("Роль");
        roleCol.setCellValueFactory(data -> data.getValue().roleProperty());
        roleCol.setPrefWidth(120);

        TableColumn<UserFX, String> statusCol = new TableColumn<>("Статус");
        statusCol.setCellValueFactory(data -> data.getValue().statusProperty());
        statusCol.setPrefWidth(100);

        table.getColumns().addAll(idCol, loginCol, nameCol, roleCol, statusCol);

        final List<UserFX> allUsers = new ArrayList<>();

        searchField.textProperty().addListener((obs, oldVal, newVal) -> {
            String filter = newVal.trim().toLowerCase();
            table.getItems().clear();
            if (filter.isEmpty()) {
                table.getItems().addAll(allUsers);
            } else {
                for (UserFX u : allUsers) {
                    if (String.valueOf(u.getId()).contains(filter) ||
                            (u.getLogin() != null && u.getLogin().toLowerCase().contains(filter)) ||
                            (u.getFullName() != null && u.getFullName().toLowerCase().contains(filter)) ||
                            (u.getRole() != null && u.getRole().toLowerCase().contains(filter)) ||
                            (u.getStatus() != null && u.getStatus().toLowerCase().contains(filter))) {
                        table.getItems().add(u);
                    }
                }
            }
        });

        resetBtn.setOnAction(e -> {
            searchField.clear();
            table.getItems().clear();
            table.getItems().addAll(allUsers);
        });

        HBox btnBox = new HBox(10);

        Button refreshBtn = new Button("Обновить");
        refreshBtn.setStyle("-fx-background-color: #6b4c3b; -fx-text-fill: white; -fx-cursor: hand;");
        refreshBtn.setOnAction(e -> {
            allUsers.clear();
            loadUsersList(allUsers, table);
        });

        Button addBtn = new Button("Добавить");
        addBtn.setStyle("-fx-background-color: #4a7c3f; -fx-text-fill: white; -fx-cursor: hand;");
        addBtn.setOnAction(e -> {
            showAddUserDialog();
            allUsers.clear();
            loadUsersList(allUsers, table);
        });

        Button editBtn = new Button("Редактировать");
        editBtn.setStyle("-fx-background-color: #c49b2a; -fx-text-fill: white; -fx-cursor: hand;");
        editBtn.setOnAction(e -> {
            UserFX selected = table.getSelectionModel().getSelectedItem();
            if (selected != null) {
                showEditUserDialog(selected);
                allUsers.clear();
                loadUsersList(allUsers, table);
            }
        });

        Button toggleBtn = new Button("Актив./Деактив.");
        toggleBtn.setStyle("-fx-background-color: #b53b3b; -fx-text-fill: white; -fx-cursor: hand;");
        toggleBtn.setOnAction(e -> {
            UserFX selected = table.getSelectionModel().getSelectedItem();
            if (selected != null) {
                try {
                    out.write("TOGGLE_USER:" + selected.getId());
                    out.newLine(); out.flush();
                    in.readLine(); in.readLine();
                    allUsers.clear();
                    loadUsersList(allUsers, table);
                } catch (IOException ex) {}
            }
        });

        btnBox.getChildren().addAll(refreshBtn, addBtn, editBtn, toggleBtn);

        loadUsersList(allUsers, table);

        vbox.getChildren().addAll(header, searchBox, table, btnBox);
        tab.setContent(vbox);
        return tab;
    }

    private void loadUsersList(List<UserFX> allUsers, TableView<UserFX> table) {
        allUsers.clear();
        table.getItems().clear();
        try {
            out.write("USERS_LIST");
            out.newLine(); out.flush();

            String line;
            while ((line = in.readLine()) != null) {
                if (line.equals("===END===")) break;
                if (line.trim().isEmpty()) continue;
                String[] parts = line.split("\\|");
                if (parts.length >= 5) {
                    UserFX u = new UserFX(
                            Integer.parseInt(parts[0]), parts[1], parts[2], parts[3], parts[4]);
                    allUsers.add(u);
                }
            }
            table.getItems().addAll(allUsers);
        } catch (IOException ex) {
            System.err.println("Ошибка загрузки пользователей");
        }
    }

    private void showAddUserDialog() {
        Dialog<ButtonType> dialog = new Dialog<>();
        dialog.setTitle("Добавить пользователя");

        ButtonType saveBtn = new ButtonType("Сохранить", ButtonBar.ButtonData.OK_DONE);
        dialog.getDialogPane().getButtonTypes().addAll(saveBtn, ButtonType.CANCEL);

        GridPane grid = new GridPane();
        grid.setHgap(10); grid.setVgap(10); grid.setPadding(new Insets(20));

        TextField loginField = new TextField();
        PasswordField passField = new PasswordField();
        TextField nameField = new TextField();
        ComboBox<String> roleCombo = new ComboBox<>();
        roleCombo.getItems().addAll("ADMIN", "MANAGER", "ANALYST");
        roleCombo.getSelectionModel().select(1);

        grid.add(new Label("Логин:"), 0, 0); grid.add(loginField, 1, 0);
        grid.add(new Label("Пароль:"), 0, 1); grid.add(passField, 1, 1);
        grid.add(new Label("ФИО:"), 0, 2); grid.add(nameField, 1, 2);
        grid.add(new Label("Роль:"), 0, 3); grid.add(roleCombo, 1, 3);

        dialog.getDialogPane().setContent(grid);

        dialog.showAndWait().ifPresent(bt -> {
            if (bt == saveBtn) {
                try {
                    String role = roleCombo.getValue();
                    int roleId = role.equals("ADMIN") ? 1 : role.equals("MANAGER") ? 2 : 3;
                    out.write("ADD_USER:" + loginField.getText().trim() + "|" +
                            passField.getText() + "|" + nameField.getText().trim() + "|" + roleId);
                    out.newLine(); out.flush();
                    in.readLine(); in.readLine();
                } catch (IOException ex) {}
            }
        });
    }

    private void showEditUserDialog(UserFX user) {
        Dialog<ButtonType> dialog = new Dialog<>();
        dialog.setTitle("Редактировать пользователя");

        ButtonType saveBtn = new ButtonType("Сохранить", ButtonBar.ButtonData.OK_DONE);
        dialog.getDialogPane().getButtonTypes().addAll(saveBtn, ButtonType.CANCEL);

        GridPane grid = new GridPane();
        grid.setHgap(10); grid.setVgap(10); grid.setPadding(new Insets(20));

        TextField nameField = new TextField(user.getFullName());
        PasswordField passField = new PasswordField();
        passField.setPromptText("Оставьте пустым, чтобы не менять");
        ComboBox<String> roleCombo = new ComboBox<>();
        roleCombo.getItems().addAll("ADMIN", "MANAGER", "ANALYST");
        roleCombo.setValue(user.getRole());

        grid.add(new Label("Логин:"), 0, 0);
        grid.add(new Label(user.getLogin()), 1, 0);
        grid.add(new Label("ФИО:"), 0, 1); grid.add(nameField, 1, 1);
        grid.add(new Label("Роль:"), 0, 2); grid.add(roleCombo, 1, 2);
        grid.add(new Label("Пароль:"), 0, 3); grid.add(passField, 1, 3);

        dialog.getDialogPane().setContent(grid);

        dialog.showAndWait().ifPresent(bt -> {
            if (bt == saveBtn) {
                try {
                    String role = roleCombo.getValue();
                    int roleId = role.equals("ADMIN") ? 1 : role.equals("MANAGER") ? 2 : 3;
                    String pass = passField.getText().isEmpty() ? "" : "|" + passField.getText();
                    out.write("UPDATE_USER:" + user.getId() + "|" + nameField.getText().trim() + "|" + roleId + pass);
                    out.newLine(); out.flush();
                    in.readLine(); in.readLine();
                } catch (IOException ex) {}
            }
        });
    }

    private Tab createAnalyticsTab(String role) {
        Tab tab = new Tab("Аналитика");

        TabPane innerPane = new TabPane();
        innerPane.setTabClosingPolicy(TabPane.TabClosingPolicy.UNAVAILABLE);

        Tab turnoverTab = new Tab("Оборачиваемость");
        turnoverTab.setContent(createTurnoverTab());

        Tab abcTab = new Tab("ABC-анализ");
        abcTab.setContent(createAbcTab());

        innerPane.getTabs().addAll(turnoverTab, abcTab);
        tab.setContent(innerPane);
        return tab;
    }

    private VBox createTurnoverTab() {
        VBox vbox = new VBox(10);
        vbox.setPadding(new Insets(15));
        vbox.setStyle("-fx-background-color: #ffffff;");

        Text header = new Text("Коэффициенты оборачиваемости");
        header.setFont(Font.font("System", FontWeight.BOLD, 16));
        header.setFill(Color.rgb(80, 50, 30));

        HBox periodBox = new HBox(10);
        periodBox.setAlignment(Pos.CENTER_LEFT);

        TextField daysField = new TextField("30");
        daysField.setPrefWidth(60);

        Button calcBtn = new Button("Рассчитать");
        calcBtn.setStyle("-fx-background-color: #6b4c3b; -fx-text-fill: white; -fx-cursor: hand;");

        Button exportBtn = new Button("Экспорт в Excel");
        exportBtn.setStyle("-fx-background-color: #4a7c3f; -fx-text-fill: white; -fx-cursor: hand;");

        periodBox.getChildren().addAll(new Label("Период (дней):"), daysField, calcBtn, exportBtn);

        TableView<TurnoverFX> table = new TableView<>();
        table.setPrefHeight(400);

        TableColumn<TurnoverFX, String> nameCol = new TableColumn<>("Товар");
        nameCol.setCellValueFactory(data -> data.getValue().nameProperty());
        nameCol.setPrefWidth(250);

        TableColumn<TurnoverFX, Number> soldCol = new TableColumn<>("Продано");
        soldCol.setCellValueFactory(data -> data.getValue().soldProperty());
        soldCol.setPrefWidth(80);

        TableColumn<TurnoverFX, Number> stockCol = new TableColumn<>("Остаток");
        stockCol.setCellValueFactory(data -> data.getValue().stockProperty());
        stockCol.setPrefWidth(80);

        TableColumn<TurnoverFX, Number> ratioCol = new TableColumn<>("Коэффициент");
        ratioCol.setCellValueFactory(data -> data.getValue().ratioProperty());
        ratioCol.setPrefWidth(100);

        table.getColumns().addAll(nameCol, soldCol, stockCol, ratioCol);

        final List<TurnoverFX> turnoverData = new ArrayList<>();

        calcBtn.setOnAction(e -> {
            try {
                int d = Integer.parseInt(daysField.getText().trim());
                System.out.println("DEBUG Turnover: days=" + d);
                loadTurnoverData(table, turnoverData, d);
            } catch (NumberFormatException ex) {
                showAlert("Ошибка", "Введите число дней");
            }
        });

        exportBtn.setOnAction(e -> {
            if (turnoverData.isEmpty()) {
                showAlert("Ошибка", "Сначала рассчитайте оборачиваемость");
                return;
            }
            exportTurnoverToExcel(turnoverData, Integer.parseInt(daysField.getText().trim()));
        });

        loadTurnoverData(table, turnoverData, 30);

        vbox.getChildren().addAll(header, periodBox, table);
        return vbox;
    }

    private void loadTurnoverData(TableView<TurnoverFX> table, List<TurnoverFX> turnoverData, int days) {
        table.getItems().clear();
        turnoverData.clear();
        try {
            out.write("TURNOVER " + days);
            out.newLine(); out.flush();

            String line;
            boolean headerSkipped = false;
            while ((line = in.readLine()) != null) {
                if (line.equals("===END===")) break;
                if (!headerSkipped && (line.startsWith("===") || line.contains("Продано") || line.startsWith("---"))) {
                    headerSkipped = true; continue;
                }
                if (line.startsWith("---") || line.startsWith("Чем выше") || line.trim().isEmpty()) continue;

                String[] parts = line.trim().split("\\s{2,}");
                if (parts.length >= 4) {
                    try {
                        Integer.parseInt(parts[1].trim());
                        String name = parts[0].trim();
                        int sold = Integer.parseInt(parts[1].trim());
                        int stock = Integer.parseInt(parts[2].trim());
                        double ratio = Double.parseDouble(parts[3].trim().replace(",", "."));
                        TurnoverFX tfx = new TurnoverFX(name, sold, stock, ratio);
                        turnoverData.add(tfx);
                        table.getItems().add(tfx);
                    } catch (NumberFormatException ex) {
                    }
                }
            }
        } catch (IOException ex) {
            System.err.println("Ошибка загрузки оборачиваемости");
        }
    }

    private int getProductIdFromCombo(ComboBox<String> combo) {
        int index = combo.getSelectionModel().getSelectedIndex();
        if (index >= 0 && combo.getUserData() instanceof List) {
            @SuppressWarnings("unchecked")
            List<String[]> items = (List<String[]>) combo.getUserData();
            if (index < items.size()) {
                return Integer.parseInt(items.get(index)[0]);
            }
        }
        return 0;
    }

    private void exportTurnoverToExcel(List<TurnoverFX> data, int days) {
        FileChooser fileChooser = new FileChooser();
        fileChooser.setTitle("Сохранить оборачиваемость");
        fileChooser.getExtensionFilters().add(new FileChooser.ExtensionFilter("Excel XML", "*.xls"));
        fileChooser.setInitialFileName("Оборачиваемость_" + days + "дней.xls");

        File file = fileChooser.showSaveDialog(null);
        if (file != null) {
            try (PrintWriter pw = new PrintWriter(new OutputStreamWriter(new FileOutputStream(file), "UTF-8"))) {
                pw.println("<?xml version=\"1.0\" encoding=\"UTF-8\"?>");
                pw.println("<?mso-application progid=\"Excel.Sheet\"?>");
                pw.println("<Workbook xmlns=\"urn:schemas-microsoft-com:office:spreadsheet\"");
                pw.println(" xmlns:ss=\"urn:schemas-microsoft-com:office:spreadsheet\">");

                pw.println("<Styles>");
                pw.println("  <Style ss:ID=\"Header\">");
                pw.println("    <Borders><Border ss:Position=\"Bottom\" ss:LineStyle=\"Continuous\" ss:Weight=\"2\"/>");
                pw.println("      <Border ss:Position=\"Left\" ss:LineStyle=\"Continuous\" ss:Weight=\"1\"/>");
                pw.println("      <Border ss:Position=\"Right\" ss:LineStyle=\"Continuous\" ss:Weight=\"1\"/>");
                pw.println("      <Border ss:Position=\"Top\" ss:LineStyle=\"Continuous\" ss:Weight=\"2\"/></Borders>");
                pw.println("    <Font ss:FontName=\"Arial\" ss:Size=\"11\" ss:Bold=\"1\" ss:Color=\"#FFFFFF\"/>");
                pw.println("    <Interior ss:Color=\"#6B4C3B\" ss:Pattern=\"Solid\"/>");
                pw.println("    <Alignment ss:Horizontal=\"Center\"/>");
                pw.println("  </Style>");
                pw.println("  <Style ss:ID=\"Title\">");
                pw.println("    <Font ss:FontName=\"Arial\" ss:Size=\"16\" ss:Bold=\"1\"/>");
                pw.println("  </Style>");
                pw.println("  <Style ss:ID=\"Default\">");
                pw.println("    <Borders><Border ss:Position=\"Bottom\" ss:LineStyle=\"Continuous\" ss:Weight=\"1\"/>");
                pw.println("      <Border ss:Position=\"Left\" ss:LineStyle=\"Continuous\" ss:Weight=\"1\"/>");
                pw.println("      <Border ss:Position=\"Right\" ss:LineStyle=\"Continuous\" ss:Weight=\"1\"/>");
                pw.println("      <Border ss:Position=\"Top\" ss:LineStyle=\"Continuous\" ss:Weight=\"1\"/></Borders>");
                pw.println("    <Font ss:FontName=\"Arial\" ss:Size=\"11\"/>");
                pw.println("  </Style>");
                pw.println("  <Style ss:ID=\"High\">");
                pw.println("    <Borders><Border ss:Position=\"Bottom\" ss:LineStyle=\"Continuous\" ss:Weight=\"1\"/>");
                pw.println("      <Border ss:Position=\"Left\" ss:LineStyle=\"Continuous\" ss:Weight=\"1\"/>");
                pw.println("      <Border ss:Position=\"Right\" ss:LineStyle=\"Continuous\" ss:Weight=\"1\"/>");
                pw.println("      <Border ss:Position=\"Top\" ss:LineStyle=\"Continuous\" ss:Weight=\"1\"/></Borders>");
                pw.println("    <Font ss:FontName=\"Arial\" ss:Size=\"11\" ss:Color=\"#2E7D32\" ss:Bold=\"1\"/>");
                pw.println("    <Interior ss:Color=\"#E8F5E9\" ss:Pattern=\"Solid\"/>");
                pw.println("  </Style>");
                pw.println("  <Style ss:ID=\"Low\">");
                pw.println("    <Borders><Border ss:Position=\"Bottom\" ss:LineStyle=\"Continuous\" ss:Weight=\"1\"/>");
                pw.println("      <Border ss:Position=\"Left\" ss:LineStyle=\"Continuous\" ss:Weight=\"1\"/>");
                pw.println("      <Border ss:Position=\"Right\" ss:LineStyle=\"Continuous\" ss:Weight=\"1\"/>");
                pw.println("      <Border ss:Position=\"Top\" ss:LineStyle=\"Continuous\" ss:Weight=\"1\"/></Borders>");
                pw.println("    <Font ss:FontName=\"Arial\" ss:Size=\"11\" ss:Color=\"#C62828\"/>");
                pw.println("    <Interior ss:Color=\"#FFEBEE\" ss:Pattern=\"Solid\"/>");
                pw.println("  </Style>");
                pw.println("</Styles>");

                pw.println("<Worksheet ss:Name=\"Оборачиваемость\">");
                pw.println("<Table>");
                pw.println("  <Column ss:Width=\"250\"/>");
                pw.println("  <Column ss:Width=\"80\"/>");
                pw.println("  <Column ss:Width=\"80\"/>");
                pw.println("  <Column ss:Width=\"100\"/>");

                pw.println("  <Row ss:Height=\"40\">");
                pw.println("    <Cell ss:MergeAcross=\"3\" ss:StyleID=\"Title\">");
                pw.println("      <Data ss:Type=\"String\">Коэффициенты оборачиваемости за " + days + " дней</Data>");
                pw.println("    </Cell>");
                pw.println("  </Row>");
                pw.println("  <Row></Row>");

                pw.println("  <Row ss:Height=\"30\">");
                pw.println("    <Cell ss:StyleID=\"Header\"><Data ss:Type=\"String\">Товар</Data></Cell>");
                pw.println("    <Cell ss:StyleID=\"Header\"><Data ss:Type=\"String\">Продано</Data></Cell>");
                pw.println("    <Cell ss:StyleID=\"Header\"><Data ss:Type=\"String\">Остаток</Data></Cell>");
                pw.println("    <Cell ss:StyleID=\"Header\"><Data ss:Type=\"String\">Коэффициент</Data></Cell>");
                pw.println("  </Row>");

                for (TurnoverFX item : data) {
                    String styleId = item.getRatio() >= 0.3 ? "High" : (item.getRatio() < 0.1 ? "Low" : "Default");
                    pw.println("  <Row>");
                    pw.println("    <Cell ss:StyleID=\"" + styleId + "\"><Data ss:Type=\"String\">" + item.getName() + "</Data></Cell>");
                    pw.println("    <Cell ss:StyleID=\"" + styleId + "\"><Data ss:Type=\"Number\">" + item.getSold() + "</Data></Cell>");
                    pw.println("    <Cell ss:StyleID=\"" + styleId + "\"><Data ss:Type=\"Number\">" + item.getStock() + "</Data></Cell>");
                    pw.println("    <Cell ss:StyleID=\"" + styleId + "\"><Data ss:Type=\"Number\">" + String.format("%.2f", item.getRatio()).replace(",", ".") + "</Data></Cell>");
                    pw.println("  </Row>");
                }

                pw.println("</Table>");
                pw.println("</Worksheet>");
                pw.println("</Workbook>");

                showAlert("Экспорт", "Файл сохранён: " + file.getName() + "\nОткройте в Excel!");
            } catch (IOException ex) {
                showAlert("Ошибка", "Не удалось сохранить файл");
            }
        }
    }

    private VBox createAbcTab() {
        VBox vbox = new VBox(10);
        vbox.setPadding(new Insets(15));
        vbox.setStyle("-fx-background-color: #ffffff;");

        Text header = new Text("ABC-анализ товаров");
        header.setFont(Font.font("System", FontWeight.BOLD, 16));
        header.setFill(Color.rgb(80, 50, 30));

        HBox periodBox = new HBox(10);
        periodBox.setAlignment(Pos.CENTER_LEFT);

        TextField daysField = new TextField("30");
        daysField.setPrefWidth(60);

        Button calcBtn = new Button("Рассчитать");
        calcBtn.setStyle("-fx-background-color: #6b4c3b; -fx-text-fill: white; -fx-cursor: hand;");

        Button exportBtn = new Button("Экспорт в Excel");
        exportBtn.setStyle("-fx-background-color: #4a7c3f; -fx-text-fill: white; -fx-cursor: hand;");

        periodBox.getChildren().addAll(new Label("Период (дней):"), daysField, calcBtn, exportBtn);

        TableView<AbcFX> table = new TableView<>();
        table.setPrefHeight(250);

        TableColumn<AbcFX, String> groupCol = new TableColumn<>("Группа");
        groupCol.setCellValueFactory(data -> data.getValue().groupProperty());
        groupCol.setPrefWidth(60);

        TableColumn<AbcFX, String> nameCol = new TableColumn<>("Товар");
        nameCol.setCellValueFactory(data -> data.getValue().nameProperty());
        nameCol.setPrefWidth(200);

        TableColumn<AbcFX, String> revenueCol = new TableColumn<>("Выручка");
        revenueCol.setCellValueFactory(data -> data.getValue().revenueProperty());
        revenueCol.setPrefWidth(100);

        TableColumn<AbcFX, String> shareCol = new TableColumn<>("Доля");
        shareCol.setCellValueFactory(data -> data.getValue().shareProperty());
        shareCol.setPrefWidth(70);

        TableColumn<AbcFX, String> cumulativeCol = new TableColumn<>("Накоп. доля");
        cumulativeCol.setCellValueFactory(data -> data.getValue().cumulativeProperty());
        cumulativeCol.setPrefWidth(90);

        table.getColumns().addAll(groupCol, nameCol, revenueCol, shareCol, cumulativeCol);

        PieChart pieChart = new PieChart();
        pieChart.setTitle("ABC-анализ (доля выручки)");
        pieChart.setPrefHeight(300);

        final List<AbcFX> abcData = new ArrayList<>();

        calcBtn.setOnAction(e -> {
            try {
                int d = Integer.parseInt(daysField.getText().trim());
                System.out.println("DEBUG ABC: days=" + d);
                loadAbcData(table, pieChart, abcData, d);
            } catch (NumberFormatException ex) {
                showAlert("Ошибка", "Введите число дней");
            }
        });

        exportBtn.setOnAction(e -> exportAbcToExcel(abcData, Integer.parseInt(daysField.getText().trim())));

        loadAbcData(table, pieChart, abcData, 30);

        Text legend = new Text("A — 80% выручки | B — 15% | C — 5%");
        legend.setFont(Font.font("System", 11));
        legend.setFill(Color.GRAY);

        vbox.getChildren().addAll(header, periodBox, table, pieChart, legend);
        return vbox;
    }

    private void loadAbcData(TableView<AbcFX> table, PieChart pieChart, List<AbcFX> abcData, int days) {
        table.getItems().clear();
        abcData.clear();
        pieChart.getData().clear();

        double aTotal = 0, bTotal = 0, cTotal = 0;

        try {
            out.write("ABC " + days);
            out.newLine(); out.flush();

            String line;
            boolean headerSkipped = false;
            while ((line = in.readLine()) != null) {
                if (line.equals("===END===")) break;
                if (!headerSkipped && (line.startsWith("===") || line.startsWith("Группа") || line.startsWith("---"))) {
                    headerSkipped = true; continue;
                }
                if (line.startsWith("---") || line.startsWith("A —") || line.startsWith("Товары группы") || line.trim().isEmpty()) continue;

                String[] parts = line.trim().split("\\s{2,}");
                if (parts.length >= 5) {
                    String group = parts[0].trim();
                    String name = parts[1].trim();
                    String revenue = parts[2].trim();
                    String share = parts[3].trim();
                    String cumulative = parts[4].trim();

                    AbcFX item = new AbcFX(group, name, revenue, share, cumulative);
                    abcData.add(item);
                    table.getItems().add(item);

                    double rev = parseDoubleSafe(revenue);
                    if (group.equals("A")) aTotal += rev;
                    else if (group.equals("B")) bTotal += rev;
                    else cTotal += rev;
                }
            }

            if (aTotal > 0) pieChart.getData().add(new PieChart.Data("A (" + formatMoney(aTotal) + ")", aTotal));
            if (bTotal > 0) pieChart.getData().add(new PieChart.Data("B (" + formatMoney(bTotal) + ")", bTotal));
            if (cTotal > 0) pieChart.getData().add(new PieChart.Data("C (" + formatMoney(cTotal) + ")", cTotal));

        } catch (IOException ex) {
            System.err.println("Ошибка загрузки ABC");
        }
    }

    private double parseDoubleSafe(String s) {
        try {
            return Double.parseDouble(s.replace(",", ".").replace("%", "").trim());
        } catch (NumberFormatException e) {
            return 0;
        }
    }

    private String formatMoney(double val) {
        return String.format("%.0f руб.", val);
    }

    private void exportAbcToExcel(List<AbcFX> data, int days) {
        FileChooser fileChooser = new FileChooser();
        fileChooser.setTitle("Сохранить ABC-анализ");
        fileChooser.getExtensionFilters().add(new FileChooser.ExtensionFilter("Excel XML", "*.xls"));
        fileChooser.setInitialFileName("ABC_анализ_" + days + "дней.xls");

        File file = fileChooser.showSaveDialog(null);
        if (file != null) {
            try (PrintWriter pw = new PrintWriter(new OutputStreamWriter(new FileOutputStream(file), "UTF-8"))) {
                pw.println("<?xml version=\"1.0\" encoding=\"UTF-8\"?>");
                pw.println("<?mso-application progid=\"Excel.Sheet\"?>");
                pw.println("<Workbook xmlns=\"urn:schemas-microsoft-com:office:spreadsheet\"");
                pw.println(" xmlns:ss=\"urn:schemas-microsoft-com:office:spreadsheet\">");

                pw.println("<Styles>");
                pw.println("  <Style ss:ID=\"Title\">");
                pw.println("    <Font ss:FontName=\"Arial\" ss:Size=\"16\" ss:Bold=\"1\"/>");
                pw.println("  </Style>");
                pw.println("  <Style ss:ID=\"Header\">");
                pw.println("    <Alignment ss:Horizontal=\"Center\" ss:Vertical=\"Center\" ss:WrapText=\"1\"/>");
                pw.println("    <Borders>");
                pw.println("      <Border ss:Position=\"Bottom\" ss:LineStyle=\"Continuous\" ss:Weight=\"2\"/>");
                pw.println("      <Border ss:Position=\"Left\" ss:LineStyle=\"Continuous\" ss:Weight=\"1\"/>");
                pw.println("      <Border ss:Position=\"Right\" ss:LineStyle=\"Continuous\" ss:Weight=\"1\"/>");
                pw.println("      <Border ss:Position=\"Top\" ss:LineStyle=\"Continuous\" ss:Weight=\"2\"/>");
                pw.println("    </Borders>");
                pw.println("    <Font ss:FontName=\"Arial\" ss:Size=\"11\" ss:Bold=\"1\" ss:Color=\"#FFFFFF\"/>");
                pw.println("    <Interior ss:Color=\"#6B4C3B\" ss:Pattern=\"Solid\"/>");
                pw.println("  </Style>");
                pw.println("  <Style ss:ID=\"Default\">");
                pw.println("    <Borders>");
                pw.println("      <Border ss:Position=\"Bottom\" ss:LineStyle=\"Continuous\" ss:Weight=\"1\"/>");
                pw.println("      <Border ss:Position=\"Left\" ss:LineStyle=\"Continuous\" ss:Weight=\"1\"/>");
                pw.println("      <Border ss:Position=\"Right\" ss:LineStyle=\"Continuous\" ss:Weight=\"1\"/>");
                pw.println("      <Border ss:Position=\"Top\" ss:LineStyle=\"Continuous\" ss:Weight=\"1\"/>");
                pw.println("    </Borders>");
                pw.println("    <Font ss:FontName=\"Arial\" ss:Size=\"11\"/>");
                pw.println("  </Style>");
                pw.println("  <Style ss:ID=\"GroupA\">");
                pw.println("    <Borders>");
                pw.println("      <Border ss:Position=\"Bottom\" ss:LineStyle=\"Continuous\" ss:Weight=\"1\"/>");
                pw.println("      <Border ss:Position=\"Left\" ss:LineStyle=\"Continuous\" ss:Weight=\"1\"/>");
                pw.println("      <Border ss:Position=\"Right\" ss:LineStyle=\"Continuous\" ss:Weight=\"1\"/>");
                pw.println("      <Border ss:Position=\"Top\" ss:LineStyle=\"Continuous\" ss:Weight=\"1\"/>");
                pw.println("    </Borders>");
                pw.println("    <Font ss:FontName=\"Arial\" ss:Size=\"11\" ss:Bold=\"1\" ss:Color=\"#2E7D32\"/>");
                pw.println("    <Interior ss:Color=\"#E8F5E9\" ss:Pattern=\"Solid\"/>");
                pw.println("  </Style>");
                pw.println("  <Style ss:ID=\"GroupB\">");
                pw.println("    <Borders>");
                pw.println("      <Border ss:Position=\"Bottom\" ss:LineStyle=\"Continuous\" ss:Weight=\"1\"/>");
                pw.println("      <Border ss:Position=\"Left\" ss:LineStyle=\"Continuous\" ss:Weight=\"1\"/>");
                pw.println("      <Border ss:Position=\"Right\" ss:LineStyle=\"Continuous\" ss:Weight=\"1\"/>");
                pw.println("      <Border ss:Position=\"Top\" ss:LineStyle=\"Continuous\" ss:Weight=\"1\"/>");
                pw.println("    </Borders>");
                pw.println("    <Font ss:FontName=\"Arial\" ss:Size=\"11\" ss:Color=\"#E65100\"/>");
                pw.println("    <Interior ss:Color=\"#FFF3E0\" ss:Pattern=\"Solid\"/>");
                pw.println("  </Style>");
                pw.println("  <Style ss:ID=\"GroupC\">");
                pw.println("    <Borders>");
                pw.println("      <Border ss:Position=\"Bottom\" ss:LineStyle=\"Continuous\" ss:Weight=\"1\"/>");
                pw.println("      <Border ss:Position=\"Left\" ss:LineStyle=\"Continuous\" ss:Weight=\"1\"/>");
                pw.println("      <Border ss:Position=\"Right\" ss:LineStyle=\"Continuous\" ss:Weight=\"1\"/>");
                pw.println("      <Border ss:Position=\"Top\" ss:LineStyle=\"Continuous\" ss:Weight=\"1\"/>");
                pw.println("    </Borders>");
                pw.println("    <Font ss:FontName=\"Arial\" ss:Size=\"11\" ss:Color=\"#C62828\"/>");
                pw.println("    <Interior ss:Color=\"#FFEBEE\" ss:Pattern=\"Solid\"/>");
                pw.println("  </Style>");
                pw.println("  <Style ss:ID=\"Footer\">");
                pw.println("    <Font ss:FontName=\"Arial\" ss:Size=\"10\" ss:Italic=\"1\" ss:Color=\"#666666\"/>");
                pw.println("  </Style>");
                pw.println("</Styles>");

                pw.println("<Worksheet ss:Name=\"ABC-анализ\">");
                pw.println("<Table>");

                pw.println("  <Column ss:Width=\"60\"/>");
                pw.println("  <Column ss:Width=\"220\"/>");
                pw.println("  <Column ss:Width=\"120\"/>");
                pw.println("  <Column ss:Width=\"80\"/>");
                pw.println("  <Column ss:Width=\"100\"/>");

                pw.println("  <Row ss:Height=\"40\">");
                pw.println("    <Cell ss:MergeAcross=\"4\" ss:StyleID=\"Title\">");
                pw.println("      <Data ss:Type=\"String\">ABC-анализ товаров за " + days + " дней</Data>");
                pw.println("    </Cell>");
                pw.println("  </Row>");
                pw.println("  <Row></Row>");

                pw.println("  <Row ss:Height=\"30\">");
                pw.println("    <Cell ss:StyleID=\"Header\"><Data ss:Type=\"String\">Группа</Data></Cell>");
                pw.println("    <Cell ss:StyleID=\"Header\"><Data ss:Type=\"String\">Товар</Data></Cell>");
                pw.println("    <Cell ss:StyleID=\"Header\"><Data ss:Type=\"String\">Выручка (руб.)</Data></Cell>");
                pw.println("    <Cell ss:StyleID=\"Header\"><Data ss:Type=\"String\">Доля</Data></Cell>");
                pw.println("    <Cell ss:StyleID=\"Header\"><Data ss:Type=\"String\">Накоп. доля</Data></Cell>");
                pw.println("  </Row>");

                for (AbcFX item : data) {
                    String styleId = "Default";
                    if ("A".equals(item.getGroup())) styleId = "GroupA";
                    else if ("B".equals(item.getGroup())) styleId = "GroupB";
                    else styleId = "GroupC";

                    String revenue = item.getRevenue().replace(",", ".").replace("%", "").trim();

                    pw.println("  <Row>");
                    pw.println("    <Cell ss:StyleID=\"" + styleId + "\"><Data ss:Type=\"String\">" + item.getGroup() + "</Data></Cell>");
                    pw.println("    <Cell ss:StyleID=\"" + styleId + "\"><Data ss:Type=\"String\">" + escapeXml(item.getName()) + "</Data></Cell>");
                    pw.println("    <Cell ss:StyleID=\"" + styleId + "\"><Data ss:Type=\"Number\">" + revenue + "</Data></Cell>");
                    pw.println("    <Cell ss:StyleID=\"" + styleId + "\"><Data ss:Type=\"String\">" + item.getShare() + "</Data></Cell>");
                    pw.println("    <Cell ss:StyleID=\"" + styleId + "\"><Data ss:Type=\"String\">" + item.getCumulative() + "</Data></Cell>");
                    pw.println("  </Row>");
                }

                pw.println("  <Row></Row>");
                pw.println("  <Row><Cell ss:StyleID=\"Footer\"><Data ss:Type=\"String\">A — 80% выручки (ключевые товары)</Data></Cell></Row>");
                pw.println("  <Row><Cell ss:StyleID=\"Footer\"><Data ss:Type=\"String\">B — 15% выручки (средние товары)</Data></Cell></Row>");
                pw.println("  <Row><Cell ss:StyleID=\"Footer\"><Data ss:Type=\"String\">C — 5% выручки (менее важные товары)</Data></Cell></Row>");

                pw.println("</Table>");
                pw.println("</Worksheet>");
                pw.println("</Workbook>");

                showAlert("Экспорт", "Файл сохранён: " + file.getName() + "\nОткройте в Excel!");
            } catch (IOException ex) {
                showAlert("Ошибка", "Не удалось сохранить файл");
            }
        }
    }

    private String escapeXml(String s) {
        if (s == null) return "";
        return s.replace("&", "&amp;")
                .replace("<", "&lt;")
                .replace(">", "&gt;")
                .replace("\"", "&quot;")
                .replace("'", "&apos;");
    }

    private Tab createReturnsTab() {
        Tab tab = new Tab("Возвраты");

        VBox vbox = new VBox(10);
        vbox.setPadding(new Insets(15));

        Text header = new Text("Оформление возврата");
        header.setFont(Font.font("System", FontWeight.BOLD, 16));

        HBox searchBox = new HBox(10);
        searchBox.setAlignment(Pos.CENTER_LEFT);

        TextField searchField = new TextField();
        searchField.setPromptText("Поиск");
        searchField.setPrefWidth(300);

        Button resetBtn = new Button("Сбросить");
        resetBtn.setStyle("-fx-background-color: #999; -fx-text-fill: white; -fx-cursor: hand;");

        searchBox.getChildren().addAll(new Label("Поиск:"), searchField, resetBtn);

        TableView<SaleFX> historyTable = new TableView<>();
        historyTable.setPrefHeight(300);

        TableColumn<SaleFX, Number> hIdCol = new TableColumn<>("№");
        hIdCol.setCellValueFactory(data -> data.getValue().idProperty());
        hIdCol.setPrefWidth(40);
        TableColumn<SaleFX, String> hDateCol = new TableColumn<>("Дата");
        hDateCol.setCellValueFactory(data -> data.getValue().dateProperty());
        hDateCol.setPrefWidth(120);
        TableColumn<SaleFX, String> hClientCol = new TableColumn<>("Клиент");
        hClientCol.setCellValueFactory(data -> data.getValue().clientProperty());
        hClientCol.setPrefWidth(150);
        TableColumn<SaleFX, String> hItemsCol = new TableColumn<>("Товары");
        hItemsCol.setCellValueFactory(data -> data.getValue().itemsProperty());
        hItemsCol.setPrefWidth(250);

        historyTable.getColumns().addAll(hIdCol, hDateCol, hClientCol, hItemsCol);

        final List<SaleFX> allReturnSales = new ArrayList<>();
        loadSalesForReturn(historyTable, allReturnSales);

        searchField.textProperty().addListener((obs, old, val) -> {
            String f = val.trim().toLowerCase();
            historyTable.getItems().clear();
            if (f.isEmpty()) {
                historyTable.getItems().addAll(allReturnSales);
            } else {
                for (SaleFX s : allReturnSales) {
                    if (String.valueOf(s.getId()).contains(f) ||
                            s.getClient().toLowerCase().contains(f) ||
                            s.getItems().toLowerCase().contains(f) ||
                            s.getDate().contains(f)) {
                        historyTable.getItems().add(s);
                    }
                }
            }
        });

        resetBtn.setOnAction(e -> {
            searchField.clear();
            historyTable.getItems().clear();
            historyTable.getItems().addAll(allReturnSales);
        });

        Text selectedInfo = new Text("Продажа не выбрана");
        selectedInfo.setFont(Font.font("System", 12));
        selectedInfo.setFill(Color.GRAY);

        ComboBox<String> productCombo = new ComboBox<>();
        productCombo.setPromptText("Выберите товар");
        productCombo.setPrefWidth(350);
        productCombo.setDisable(true);

        historyTable.getSelectionModel().selectedItemProperty().addListener((obs, old, val) -> {
            productCombo.getItems().clear();
            productCombo.setUserData(null);
            if (val != null) {
                try {
                    out.write("GET_SALE_ITEMS:" + val.getId());
                    out.newLine(); out.flush();

                    List<String[]> items = new ArrayList<>();
                    String line;
                    while ((line = in.readLine()) != null) {
                        if (line.equals("===END===")) break;
                        if (line.trim().isEmpty()) continue;
                        String[] parts = line.split("\\|");
                        if (parts.length >= 3) {
                            items.add(parts);
                            productCombo.getItems().add(parts[1] + " (было: " + parts[2] + " шт.) [" + parts[0] + "]");
                        }
                    }
                    productCombo.setUserData(items);
                    productCombo.setDisable(false);
                    if (!items.isEmpty()) productCombo.getSelectionModel().select(0);

                    out.write("GET_RETURN_INFO:" + val.getId());
                    out.newLine(); out.flush();
                    String returnInfo = in.readLine();
                    in.readLine();

                    if (returnInfo != null && !returnInfo.equals("Нет возвратов") && !returnInfo.isEmpty()) {
                        selectedInfo.setText("Продажа №" + val.getId() + " | " + val.getDate() + "\n⚠ Возвраты: " + returnInfo);
                        selectedInfo.setFill(Color.RED);
                    } else {
                        selectedInfo.setText("Продажа №" + val.getId() + " | " + val.getDate());
                        selectedInfo.setFill(Color.GRAY);
                    }
                } catch (IOException ex) {
                    System.err.println("Ошибка загрузки товаров продажи");
                }
            } else {
                selectedInfo.setText("Продажа не выбрана");
                selectedInfo.setFill(Color.GRAY);
                productCombo.setDisable(true);
            }
        });

        HBox returnBox = new HBox(10);
        returnBox.setAlignment(Pos.CENTER_LEFT);

        TextField qtyField = new TextField();
        qtyField.setPromptText("Кол-во");
        qtyField.setPrefWidth(60);

        TextField reasonField = new TextField();
        reasonField.setPromptText("Причина возврата");
        reasonField.setPrefWidth(200);

        Button returnBtn = new Button("Оформить возврат");
        returnBtn.setStyle("-fx-background-color: #b53b3b; -fx-text-fill: white; -fx-cursor: hand;");

        returnBox.getChildren().addAll(
                new Label("Товар:"), productCombo,
                new Label("Кол-во:"), qtyField,
                new Label("Причина:"), reasonField, returnBtn
        );

        returnBtn.setOnAction(e -> {
            SaleFX selected = historyTable.getSelectionModel().getSelectedItem();
            int productId = getProductIdFromCombo(productCombo);

            if (selected == null) { showAlert("Ошибка", "Выберите продажу"); return; }
            if (productId == 0) { showAlert("Ошибка", "Выберите товар"); return; }

            try {
                int qty = Integer.parseInt(qtyField.getText().trim());
                if (qty <= 0) throw new NumberFormatException();

                @SuppressWarnings("unchecked")
                List<String[]> items = (List<String[]>) productCombo.getUserData();
                int maxQty = 0;
                for (String[] item : items) {
                    if (Integer.parseInt(item[0]) == productId) {
                        maxQty = Integer.parseInt(item[2]);
                        break;
                    }
                }
                if (qty > maxQty) {
                    showAlert("Ошибка", "Максимальное количество для возврата: " + maxQty);
                    return;
                }

                out.write("RETURN:" + selected.getId() + "|" + productId + "|" + qty + "|" + reasonField.getText().trim());
                out.newLine(); out.flush();
                String resp = in.readLine(); in.readLine();
                if (resp.contains("оформлен")) {
                    showAlert("Успех", "Возврат оформлен. Товар возвращён на склад.");
                    allReturnSales.clear();
                    loadSalesForReturn(historyTable, allReturnSales);
                    qtyField.clear(); reasonField.clear();
                } else {
                    showAlert("Ошибка", resp);
                }
            } catch (NumberFormatException ex) {
                showAlert("Ошибка", "Введите корректное количество");
            } catch (IOException ex) {
                showAlert("Ошибка", "Ошибка связи");
            }
        });

        Button refreshBtn = new Button("Обновить");
        refreshBtn.setStyle("-fx-background-color: #6b4c3b; -fx-text-fill: white; -fx-cursor: hand;");
        refreshBtn.setOnAction(ev -> {
            allReturnSales.clear();
            loadSalesForReturn(historyTable, allReturnSales);
        });

        vbox.getChildren().addAll(header, searchBox, historyTable, selectedInfo, returnBox, refreshBtn);
        tab.setContent(vbox);
        return tab;
    }

    private void loadSalesForReturn(TableView<SaleFX> table, List<SaleFX> allSales) {
        table.getItems().clear();
        allSales.clear();
        try {
            out.write("SALES_HISTORY");
            out.newLine(); out.flush();
            String line;
            while ((line = in.readLine()) != null) {
                if (line.equals("===END===")) break;
                try {
                    if (line.startsWith("[Продажа #")) {
                        int idStart = line.indexOf("#") + 1;
                        int idEnd = line.indexOf("]");
                        int id = Integer.parseInt(line.substring(idStart, idEnd));
                        String afterId = line.substring(idEnd + 2);
                        String[] parts = afterId.split(" \\| ");
                        String date = parts[0].trim();
                        String client = parts.length > 1 ? parts[1].replace("Клиент: ", "").trim() : "";
                        double amount = 0; String items = "";
                        if (parts.length > 3) {
                            amount = Double.parseDouble(parts[3].replace("Сумма: ", "").replace(" руб.", "").replace(",", ".").trim());
                        }
                        if (parts.length > 4) items = parts[4].replace("Товары: ", "").trim();
                        SaleFX sale = new SaleFX(id, date, client, "", amount, items);
                        allSales.add(sale);
                    }
                } catch (Exception ex) {}
            }
            table.getItems().addAll(allSales);
        } catch (IOException ex) {}
    }

    private Tab createCategoriesTab(String role) {
        Tab tab = new Tab("Категории");
        VBox vbox = new VBox(10);
        vbox.setPadding(new Insets(10));
        vbox.setStyle("-fx-background-color: #ffffff;");

        Text header = new Text("Список категорий");
        header.setFont(Font.font("System", FontWeight.BOLD, 16));
        header.setFill(Color.rgb(80, 50, 30));

        HBox searchBox = new HBox(10);
        searchBox.setAlignment(Pos.CENTER_LEFT);
        TextField searchField = new TextField();
        searchField.setPromptText("Поиск");
        searchField.setPrefWidth(300);
        Button resetBtn = new Button("Сбросить");
        resetBtn.setStyle("-fx-background-color: #999; -fx-text-fill: white; -fx-cursor: hand;");
        searchBox.getChildren().addAll(new Label("Поиск:"), searchField, resetBtn);

        TableView<CategoryFX> table = new TableView<>();
        table.setPrefHeight(380);
        table.setStyle("-fx-font-family: 'System'; -fx-font-size: 12px;");

        TableColumn<CategoryFX, Number> idCol = new TableColumn<>("ID");
        idCol.setCellValueFactory(data -> data.getValue().idProperty());
        idCol.setPrefWidth(40);
        TableColumn<CategoryFX, String> nameCol = new TableColumn<>("Название");
        nameCol.setCellValueFactory(data -> data.getValue().nameProperty());
        nameCol.setPrefWidth(250);
        TableColumn<CategoryFX, String> descCol = new TableColumn<>("Описание");
        descCol.setCellValueFactory(data -> data.getValue().descriptionProperty());
        descCol.setPrefWidth(400);
        table.getColumns().addAll(idCol, nameCol, descCol);

        final List<CategoryFX> allCats = new ArrayList<>();

        searchField.textProperty().addListener((obs, oldVal, newVal) -> {
            String filter = newVal.trim().toLowerCase();
            table.getItems().clear();
            if (filter.isEmpty()) {
                table.getItems().addAll(allCats);
            } else {
                for (CategoryFX c : allCats) {
                    if (String.valueOf(c.getId()).contains(filter) ||
                            (c.getName() != null && c.getName().toLowerCase().contains(filter)) ||
                            (c.getDescription() != null && c.getDescription().toLowerCase().contains(filter))) {
                        table.getItems().add(c);
                    }
                }
            }
        });

        resetBtn.setOnAction(e -> {
            searchField.clear();
            table.getItems().clear();
            table.getItems().addAll(allCats);
        });

        HBox btnBox = new HBox(10);

        Button refreshBtn = new Button("Обновить");
        refreshBtn.setStyle("-fx-background-color: #6b4c3b; -fx-text-fill: white; -fx-cursor: hand;");
        refreshBtn.setOnAction(e -> {
            allCats.clear();
            loadCategoriesList(allCats, table);
        });

        Button addBtn = new Button("Добавить");
        addBtn.setStyle("-fx-background-color: #4a7c3f; -fx-text-fill: white; -fx-cursor: hand;");
        addBtn.setOnAction(e -> {
            showAddCategoryDialog();
            allCats.clear();
            loadCategoriesList(allCats, table);
        });

        Button editBtn = new Button("Редактировать");
        editBtn.setStyle("-fx-background-color: #c49b2a; -fx-text-fill: white; -fx-cursor: hand;");
        editBtn.setOnAction(e -> {
            CategoryFX sel = table.getSelectionModel().getSelectedItem();
            if (sel != null) {
                showEditCategoryDialog(sel);
                allCats.clear();
                loadCategoriesList(allCats, table);
            }
        });

        Button delBtn = new Button("Удалить");
        if (!"ADMIN".equals(role)) {
            delBtn.setVisible(false);
        }
        delBtn.setStyle("-fx-background-color: #b53b3b; -fx-text-fill: white; -fx-cursor: hand;");
        delBtn.setOnAction(e -> {
            CategoryFX sel = table.getSelectionModel().getSelectedItem();
            if (sel != null) {
                try {
                    out.write("DELETE_CATEGORY:" + sel.getId());
                    out.newLine(); out.flush();
                    String resp = in.readLine();
                    System.out.println("DEBUG: server response=" + resp);
                    String endLine = in.readLine();
                    if (resp != null && resp.contains("удалена")) {
                        showAlert("Успех", resp);
                        reloadAllCategoryCombos();
                    }
                    else if (resp != null && resp.contains("Ошибка")) showAlert("Ошибка", resp.replace("Ошибка: ", ""));
                    else showAlert("Результат", resp);
                    allCats.clear();
                    loadCategoriesList(allCats, table);
                } catch (IOException ex) {}
            }
        });

        btnBox.getChildren().addAll(refreshBtn, addBtn, editBtn, delBtn);

        loadCategoriesList(allCats, table);

        vbox.getChildren().addAll(header, searchBox, table, btnBox);
        tab.setContent(vbox);
        return tab;
    }

    private void loadCategoriesList(List<CategoryFX> allCats, TableView<CategoryFX> table) {
        allCats.clear();
        table.getItems().clear();
        try {
            out.write("CATEGORIES_FULL"); out.newLine(); out.flush();
            String line;
            while ((line = in.readLine()) != null) {
                if (line.equals("===END===")) break;
                if (line.trim().isEmpty()) continue;
                String[] p = line.split("\\|");
                if (p.length >= 3) {
                    CategoryFX c = new CategoryFX(Integer.parseInt(p[0]), p[1], p[2]);
                    allCats.add(c);
                }
            }
            table.getItems().addAll(allCats);
        } catch (IOException ex) {}
    }

    private void showAddCategoryDialog() {
        Dialog<ButtonType> d = new Dialog<>();
        d.setTitle("Добавить категорию");
        GridPane g = new GridPane(); g.setHgap(10); g.setVgap(10); g.setPadding(new Insets(20));
        TextField n = new TextField(); TextField de = new TextField();
        g.add(new Label("Название:"), 0, 0); g.add(n, 1, 0);
        g.add(new Label("Описание:"), 0, 1); g.add(de, 1, 1);
        d.getDialogPane().setContent(g);
        ButtonType s = new ButtonType("Сохранить", ButtonBar.ButtonData.OK_DONE);
        d.getDialogPane().getButtonTypes().addAll(s, ButtonType.CANCEL);
        d.showAndWait().ifPresent(b -> {
            if (b == s && !n.getText().trim().isEmpty()) {
                try {
                    out.write("ADD_CATEGORY:" + n.getText().trim() + "|" + de.getText().trim());
                    out.newLine(); out.flush();
                    String resp = in.readLine(); in.readLine();
                    if (resp.contains("добавлена")) {
                        showAlert("Успех", "Категория добавлена");
                        reloadAllCategoryCombos();
                    }
                } catch (IOException ex) {}
            }
        });
    }

    private void showEditCategoryDialog(CategoryFX cat) {
        Dialog<ButtonType> d = new Dialog<>();
        d.setTitle("Редактировать категорию");
        GridPane g = new GridPane(); g.setHgap(10); g.setVgap(10); g.setPadding(new Insets(20));
        TextField n = new TextField(cat.getName()); TextField de = new TextField(cat.getDescription());
        g.add(new Label("Название:"), 0, 0); g.add(n, 1, 0);
        g.add(new Label("Описание:"), 0, 1); g.add(de, 1, 1);
        d.getDialogPane().setContent(g);
        ButtonType s = new ButtonType("Сохранить", ButtonBar.ButtonData.OK_DONE);
        d.getDialogPane().getButtonTypes().addAll(s, ButtonType.CANCEL);
        d.showAndWait().ifPresent(b -> {
            if (b == s) {
                try {
                    out.write("UPDATE_CATEGORY:" + cat.getId() + "|" + n.getText().trim() + "|" + de.getText().trim());
                    out.newLine(); out.flush();
                    String resp = in.readLine(); in.readLine();
                    if (resp.contains("обновлена")) {
                        showAlert("Успех", "Категория обновлена");
                        reloadAllCategoryCombos();
                    }
                } catch (IOException ex) {}
            }
        });
    }

    private void reloadAllCategoryCombos() {
        for (ComboBox<String> combo : categoryCombos) {
            String selected = combo.getSelectionModel().getSelectedItem();
            combo.getItems().clear();
            combo.getItems().add("Все категории");
            loadCategories(combo);
            if (selected != null && combo.getItems().contains(selected)) {
                combo.getSelectionModel().select(selected);
            } else {
                combo.getSelectionModel().select(0);
            }
        }
    }

    private void reloadAllClientCombos() {
        for (ComboBox<String> combo : clientCombos) {
            String text = combo.getEditor() != null ? combo.getEditor().getText() : "";
            combo.getItems().clear();
            loadClients(combo);
            if (!text.isEmpty()) combo.getEditor().setText(text);
        }
    }

    private void reloadAllSupplierCombos() {
        for (ComboBox<String> combo : supplierCombos) {
            String text = combo.getEditor() != null ? combo.getEditor().getText() : "";
            combo.getItems().clear();
            loadSuppliers(combo);
            if (!text.isEmpty()) combo.getEditor().setText(text);
        }
    }

    private void closeConnection() {
        try {
            if (in != null) in.close();
            if (out != null) out.close();
            if (socket != null && !socket.isClosed()) socket.close();
        } catch (IOException e) {
        }
    }

    @Override
    public void stop() {
        closeConnection();
    }
}