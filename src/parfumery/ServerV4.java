package parfumery;

import java.io.*;
import java.net.*;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class ServerV4 {

    private int getPort() {
        return DatabaseConfig.getServerPort();
    }

    private int getThreadPoolSize() {
        return DatabaseConfig.getServerThreads();
    }

    private ExecutorService threadPool;
    private volatile boolean running = true;

    public void start() {
        System.out.println("=== Сервер парфюмерного магазина v4 (многопоточный) ===");
        System.out.println("Пул потоков: " + getThreadPoolSize() + " потоков");

        threadPool = Executors.newFixedThreadPool(getThreadPoolSize());

        System.out.println("Ожидаю подключения клиентов на порту " + getPort() + "...");

        DatabaseManager dbManager = new DatabaseManager();
        try {
            dbManager.connect();
            List<Product> products = dbManager.getAllProducts();
            DataExporter.saveProducts(products, "products_backup.ser");
        } catch (Exception e) {
            System.err.println("Не удалось создать резервную копию: " + e.getMessage());
        } finally {
            dbManager.disconnect();
        }

        try (ServerSocket serverSocket = new ServerSocket(getPort())) {

            while (running) {
                try {
                    Socket clientSocket = serverSocket.accept();
                    System.out.println("[+] Новый клиент: " + clientSocket.getInetAddress());

                    threadPool.execute(new ClientHandler(clientSocket));

                    System.out.println("[i] Активных потоков: " +
                            ((java.util.concurrent.ThreadPoolExecutor)threadPool).getActiveCount());

                } catch (IOException e) {
                    if (running) {
                        System.err.println("Ошибка принятия подключения: " + e.getMessage());
                    }
                }
            }

        } catch (IOException e) {
            System.err.println("Ошибка сервера: " + e.getMessage());
        } finally {
            shutdown();
        }

        System.out.println("=== Сервер остановлен ===");
    }

    private void shutdown() {
        running = false;
        if (threadPool != null) {
            threadPool.shutdown();
            System.out.println("Пул потоков остановлен.");
        }
    }

    private class ClientHandler implements Runnable {

        private Socket clientSocket;

        public ClientHandler(Socket socket) {
            this.clientSocket = socket;
        }

        @Override
        public void run() {
            DatabaseManager dbManager = new DatabaseManager();

            try {
                dbManager.connect();
            } catch (Exception e) {
                System.err.println("[!] Поток " + Thread.currentThread().getName() +
                        ": не удалось подключиться к БД: " + e.getMessage());
                closeSocket();
                return;
            }

            try (BufferedReader in = new BufferedReader(
                    new InputStreamReader(clientSocket.getInputStream(), "UTF-8"));
                 BufferedWriter out = new BufferedWriter(
                         new OutputStreamWriter(clientSocket.getOutputStream(), "UTF-8"))) {

                User currentUser = null;

                while (currentUser == null && running) {
                    String authRequest = in.readLine();
                    if (authRequest == null) {
                        System.out.println("[-] Клиент отключился до авторизации: " +
                                clientSocket.getInetAddress());
                        return;
                    }

                    System.out.println("[" + Thread.currentThread().getName() +
                            "] Запрос авторизации: " + authRequest);

                    if (authRequest.toUpperCase().startsWith("LOGIN:")) {
                        String[] parts = authRequest.split(":");
                        if (parts.length >= 3) {
                            try {
                                currentUser = dbManager.authenticate(parts[1], parts[2]);
                                if (currentUser != null) {
                                    out.write("OK:" + currentUser.getRoleName() + ":" +
                                            currentUser.getFullName());
                                    out.newLine();
                                    out.flush();
                                    System.out.println("[+] " + currentUser.getLogin() +
                                            " авторизован в потоке " + Thread.currentThread().getName());
                                } else {
                                    out.write("ERROR:Неверный логин или пароль");
                                    out.newLine();
                                    out.flush();
                                }
                            } catch (Exception e) {
                                out.write("ERROR:Ошибка сервера");
                                out.newLine();
                                out.flush();
                            }
                        } else {
                            out.write("ERROR:Формат: LOGIN:логин:пароль");
                            out.newLine();
                            out.flush();
                        }
                    } else if (authRequest.trim().equalsIgnoreCase("EXIT")) {
                        out.write("BYE");
                        out.newLine();
                        out.flush();
                        return;
                    } else {
                        out.write("ERROR:Авторизуйтесь: LOGIN:логин:пароль");
                        out.newLine();
                        out.flush();
                    }
                }

                if (!running) return;

                out.write("READY");
                out.newLine();
                out.flush();

                String request;
                while ((request = in.readLine()) != null && running) {
                    System.out.println("[" + Thread.currentThread().getName() +
                            "] " + currentUser.getLogin() + ": " + request);

                    String response = processRequest(request, currentUser, dbManager);

                    out.write(response);
                    out.newLine();
                    out.write("===END===");
                    out.newLine();
                    out.flush();

                    if (request.trim().equalsIgnoreCase("EXIT")) {
                        System.out.println("[-] " + currentUser.getLogin() + " отключился.");
                        break;
                    }
                }

            } catch (IOException e) {
                System.err.println("[!] Ошибка связи с клиентом: " + e.getMessage());
            } finally {
                dbManager.disconnect();
                closeSocket();
            }
        }

        private void closeSocket() {
            try {
                if (clientSocket != null && !clientSocket.isClosed()) {
                    clientSocket.close();
                }
            } catch (IOException e) {
            }
        }
    }

    private String processRequest(String request, User user, DatabaseManager dbManager) {
        String command = request.trim().toUpperCase();

        try {
            if (command.equals("LIST")) {
                List<Product> products = dbManager.getAllProducts();
                if (products.isEmpty()) return "В базе нет товаров.";

                StringBuilder sb = new StringBuilder("=== Список всех товаров ===\n");
                for (Product p : products) {
                    sb.append(String.format(
                            "[%d] %s|%s|%d|%s|%.2f|%.2f|%d|%s\n",
                            p.getId(),
                            p.getName(),
                            p.getBrand() != null ? p.getBrand() : "-",
                            p.getVolumeMl(),
                            p.getCategoryName() != null ? p.getCategoryName() : "-",
                            p.getPurchasePrice() != null ? p.getPurchasePrice().doubleValue() : 0.0,
                            p.getRetailPrice() != null ? p.getRetailPrice().doubleValue() : 0.0,
                            p.getStockQuantity(),
                            p.getDescription() != null ? p.getDescription() : ""
                    ));
                }
                sb.append("Всего товаров: ").append(products.size());
                return sb.toString();
            }

            if (command.equals("REPORT")) {
                List<Product> products = dbManager.getAllProducts();
                if (products.isEmpty()) return "В базе нет товаров.";

                StringBuilder sb = new StringBuilder();
                sb.append("=== Отчёт по товарам ===\n");
                sb.append(String.format("%-5s %-25s %-15s %-10s %-10s %-10s\n",
                        "ID", "Название", "Бренд", "Объём", "Цена", "Остаток"));
                sb.append("-".repeat(80)).append("\n");

                for (Product p : products) {
                    sb.append(String.format("%-5d %-25s %-15s %-8d мл %-8.2f %-8d шт.\n",
                            p.getId(),
                            p.getName(),
                            p.getBrand() != null ? p.getBrand() : "-",
                            p.getVolumeMl(),
                            p.getRetailPrice(),
                            p.getStockQuantity()));
                }
                sb.append("-".repeat(80)).append("\n");
                sb.append("Всего товаров: ").append(products.size()).append("\n");
                sb.append("Общая сумма (розница): ").append(
                        products.stream()
                                .mapToDouble(p -> p.getRetailPrice().doubleValue() * p.getStockQuantity())
                                .sum()
                ).append(" руб.");

                return sb.toString();
            }

            if (command.startsWith("CATEGORY ")) {
                String[] parts = request.trim().split(" ");
                if (parts.length < 2) return "Ошибка: укажите ID категории.";
                int categoryId = Integer.parseInt(parts[1]);
                List<Product> products = dbManager.getProductsByCategory(categoryId);
                if (products.isEmpty()) return "В категории с ID=" + categoryId + " нет товаров.";
                StringBuilder sb = new StringBuilder("=== Категория: " +
                        products.get(0).getCategoryName() + " ===\n");
                for (Product p : products) sb.append(p.toString()).append("\n");
                sb.append("Товаров: ").append(products.size());
                return sb.toString();
            }

            if (command.equals("WHOAMI")) {
                return "Вы: " + user.toString() + " | Поток: " + Thread.currentThread().getName();
            }

            if (command.equals("HELP")) {
                StringBuilder sb = new StringBuilder("=== Доступные команды ===\n");
                sb.append("  LIST             — все товары\n");
                sb.append("  REPORT           — отчёт по товарам\n");
                sb.append("  CATEGORY N       — товары категории N\n");
                sb.append("  CLIENTS          — список клиентов\n");
                sb.append("  WHOAMI           — информация о пользователе\n");
                if (user.isAdmin() || user.isManager()) {
                    sb.append("  SALE             — оформить продажу\n");
                    sb.append("  SUPPLY           — оформить поставку\n");
                    sb.append("  SALES_HISTORY    — история продаж\n");
                    sb.append("  SUPPLIES_HISTORY — история поставок\n");

                    sb.append("  ADD_PRODUCT      — добавить товар\n");
                    sb.append("  UPDATE_PRODUCT   — обновить товар\n");
                    if (user.isAdmin()) {
                        sb.append("  DELETE_PRODUCT   — деактивировать товар\n");
                    }
                }
                if (user.isAdmin() || user.isManager() || user.isAnalyst()) {
                    sb.append("  TURNOVER N       — оборачиваемость за N дней\n");
                    sb.append("  ABC N            — ABC-анализ за N дней\n");
                }
                if (user.isAdmin()) {
                    sb.append("  USERS            — список пользователей\n");
                    sb.append("  SHUTDOWN         — остановка сервера\n");
                }
                sb.append("  HELP             — справка\n");
                sb.append("  EXIT             — выход");
                return sb.toString();
            }

            if (command.equals("CLIENTS")) {
                List<String[]> clients = dbManager.getAllClientsWithDetails();
                StringBuilder sb = new StringBuilder();
                for (String[] c : clients) {
                    sb.append(c[0]).append("|").append(c[1]).append("|").append(c[2])
                            .append("|").append(c[3]).append("|").append(c[4]).append("\n");
                }
                return sb.toString();
            }

            if (command.startsWith("SALE:")) {
                if (!user.isAdmin() && !user.isManager()) {
                    return "Ошибка: требуется роль ADMIN или MANAGER.";
                }
                String afterCommand = request.substring(5);
                String[] mainParts = afterCommand.split(":", 2);
                if (mainParts.length < 2) {
                    return "Формат: SALE:clientId:productId:qty:price,productId:qty:price\n" +
                            "Примеры:\n" +
                            "  SALE:0:1:1:8900          (одна позиция)\n" +
                            "  SALE:1:1:1:8900,3:2:8200 (несколько позиций)";
                }

                Integer clientId = mainParts[0].equals("0") ? null : Integer.parseInt(mainParts[0]);
                String itemsPart = mainParts[1];

                String[] itemStrings = itemsPart.split(",");
                List<String[]> items = new ArrayList<>();

                for (String itemStr : itemStrings) {
                    String[] itemParts = itemStr.split(":");
                    if (itemParts.length != 3) {
                        return "Ошибка в позиции: '" + itemStr + "'. Формат: productId:quantity:price";
                    }
                    items.add(new String[]{itemParts[0], itemParts[1], itemParts[2]});
                }

                int saleId = dbManager.createSale(user.getId(), clientId, items);
                return "Продажа #" + saleId + " успешно оформлена!";
            }

            if (command.startsWith("SUPPLY:")) {
                if (!user.isAdmin() && !user.isManager()) {
                    return "Ошибка: требуется роль ADMIN или MANAGER.";
                }
                String afterCommand = request.substring(7);
                String[] mainParts = afterCommand.split(":", 3);
                if (mainParts.length < 3) {
                    return "Формат: SUPPLY:supplierId:supplierName:productId:qty:price,productId:qty:price";
                }

                int supplierId = Integer.parseInt(mainParts[0]);
                String supplierName = mainParts[1];
                String itemsPart = mainParts[2];

                String[] itemStrings = itemsPart.split(",");
                List<String[]> items = new ArrayList<>();

                for (String itemStr : itemStrings) {
                    String[] itemParts = itemStr.split(":");
                    if (itemParts.length != 3) {
                        return "Ошибка в позиции: '" + itemStr + "'";
                    }
                    items.add(new String[]{itemParts[0], itemParts[1], itemParts[2]});
                }

                int supplyId = dbManager.createSupply(supplierId, supplierName, user.getId(), items);
                return "Поставка #" + supplyId + " успешно оформлена!";
            }

            if (command.equals("SALES_HISTORY")) {
                if (!user.isAdmin() && !user.isManager()) {
                    return "Ошибка: требуется роль ADMIN или MANAGER.";
                }
                List<String> history = dbManager.getSalesHistory();
                if (history.isEmpty()) return "История продаж пуста.";
                StringBuilder sb = new StringBuilder("=== История продаж ===\n");
                for (String h : history) sb.append(h).append("\n");
                return sb.toString();
            }

            if (command.equals("SUPPLIES_HISTORY")) {
                if (!user.isAdmin() && !user.isManager()) {
                    return "Ошибка: требуется роль ADMIN или MANAGER.";
                }
                List<String> history = dbManager.getSuppliesHistory();
                if (history.isEmpty()) return "История поставок пуста.";
                StringBuilder sb = new StringBuilder("=== История поставок ===\n");
                for (String h : history) sb.append(h).append("\n");
                return sb.toString();
            }

            if (command.equals("USERS")) {
                if (!user.isAdmin()) return "Ошибка: требуется роль ADMIN.";
                List<User> users = dbManager.getAllUsers();
                StringBuilder sb = new StringBuilder("=== Список пользователей ===\n");
                for (User u : users) sb.append("  ").append(u.toString()).append("\n");
                sb.append("Всего: ").append(users.size());
                return sb.toString();
            }

            if (command.equals("SHUTDOWN")) {
                if (!user.isAdmin()) return "Ошибка: требуется роль ADMIN.";
                System.out.println("[!] Администратор " + user.getLogin() + " запросил остановку!");
                running = false;
                return "Сервер останавливается...";
            }

            if (command.startsWith("TURNOVER")) {
                if (!user.isAdmin() && !user.isManager() && !user.isAnalyst()) {
                    return "Ошибка: недостаточно прав.";
                }
                int days = 30;
                if (command.startsWith("TURNOVER ")) {
                    try {
                        days = Integer.parseInt(command.substring(9).trim());
                    } catch (NumberFormatException e) {
                        return "Формат: TURNOVER или TURNOVER N (где N — дней)";
                    }
                }

                List<String[]> data = dbManager.getTurnover(days);
                if (data.isEmpty()) return "Нет данных о продажах за " + days + " дн.";

                StringBuilder sb = new StringBuilder();
                sb.append("=== Оборачиваемость за " + days + " дн. ===\n");
                sb.append(String.format("%-35s %8s %12s %14s\n", "Товар", "Продано", "Остаток", "Коэффициент"));
                sb.append("-".repeat(75)).append("\n");

                for (String[] row : data) {
                    sb.append(String.format("%-35s %8s %12s %14s\n",
                            row[0], row[1], row[2], row[3]));
                }
                sb.append("-".repeat(75)).append("\n");
                sb.append("Чем выше коэффициент — тем быстрее продаётся товар.");
                return sb.toString();
            }

            if (command.startsWith("ABC")) {
                if (!user.isAdmin() && !user.isManager() && !user.isAnalyst()) {
                    return "Ошибка: недостаточно прав.";
                }
                int days = 30;
                if (command.startsWith("ABC ")) {
                    try {
                        days = Integer.parseInt(command.substring(4).trim());
                    } catch (NumberFormatException e) {
                        return "Формат: ABC или ABC N (где N — дней)";
                    }
                }

                List<String[]> data = dbManager.getAbcAnalysis(days);
                if (data.isEmpty()) return "Нет данных о продажах за " + days + " дн.";

                StringBuilder sb = new StringBuilder();
                sb.append("=== ABC-анализ за " + days + " дн. ===\n");
                sb.append(String.format("%-5s %-35s %12s %8s %12s\n",
                        "Группа", "Товар", "Выручка", "Доля", "Накоп. доля"));
                sb.append("-".repeat(80)).append("\n");

                for (String[] row : data) {
                    sb.append(String.format("%-5s %-35s %12s %8s %12s\n",
                            row[0], row[1], row[2], row[3], row[4]));
                }
                sb.append("-".repeat(80)).append("\n");
                sb.append("A — 80% выручки | B — 15% | C — 5%\n");

                sb.append("\nТовары группы A (ключевые): ");
                boolean first = true;
                for (String[] row : data) {
                    if ("A".equals(row[0])) {
                        if (!first) sb.append(", ");
                        sb.append(row[1]);
                        first = false;
                    }
                }

                return sb.toString();
            }

            if (command.startsWith("ADD_PRODUCT:")) {
                if (!user.isAdmin() && !user.isManager()) return "Ошибка: недостаточно прав.";
                String data = request.substring(12);
                String[] parts = data.split("\\|");
                if (parts.length < 8) return "Ошибка: неверный формат.";
                dbManager.addProduct(parts[0], Integer.parseInt(parts[1]), parts[2],
                        Integer.parseInt(parts[3]), parseDouble(parts[4]), parseDouble(parts[5]),
                        Integer.parseInt(parts[6]), parts[7]);
                return "Товар добавлен.";
            }

            if (command.startsWith("UPDATE_PRODUCT:")) {
                if (!user.isAdmin() && !user.isManager()) return "Ошибка: недостаточно прав.";
                String data = request.substring(15);
                String[] parts = data.split("\\|");
                if (parts.length < 9) return "Ошибка: неверный формат.";
                dbManager.updateProduct(Integer.parseInt(parts[0]), parts[1],
                        Integer.parseInt(parts[2]), parts[3], Integer.parseInt(parts[4]),
                        parseDouble(parts[5]), parseDouble(parts[6]), Integer.parseInt(parts[7]), parts[8]);
                return "Товар обновлён.";
            }

            if (command.startsWith("DELETE_PRODUCT:")) {
                if (!user.isAdmin()) return "Ошибка: требуется ADMIN.";
                int id = Integer.parseInt(request.substring(15));
                dbManager.deactivateProduct(id);
                return "Товар деактивирован.";
            }

            if (command.equals("CATEGORIES_LIST")) {
                List<String[]> cats = dbManager.getCategoriesList();
                StringBuilder sb = new StringBuilder();
                for (String[] c : cats) {
                    sb.append(c[0]).append("|").append(c[1]).append("\n");
                }
                return sb.toString();
            }

            if (command.startsWith("FILTER_PRODUCTS:")) {
                String data = request.substring(16);
                String[] parts = data.split("\\|", 2);
                int catId = Integer.parseInt(parts[0]);
                String search = parts.length > 1 ? parts[1] : "";
                List<Product> products = dbManager.getProductsFiltered(catId, search);
                if (products.isEmpty()) return "Товары не найдены.";
                StringBuilder sb = new StringBuilder("=== Результаты поиска ===\n");
                for (Product p : products) {
                    sb.append(String.format(
                            "[%d] %s|%s|%d|%s|%.2f|%.2f|%d|%s\n",
                            p.getId(),
                            p.getName(),
                            p.getBrand() != null ? p.getBrand() : "-",
                            p.getVolumeMl(),
                            p.getCategoryName() != null ? p.getCategoryName() : "-",
                            p.getPurchasePrice() != null ? p.getPurchasePrice().doubleValue() : 0.0,
                            p.getRetailPrice() != null ? p.getRetailPrice().doubleValue() : 0.0,
                            p.getStockQuantity(),
                            p.getDescription() != null ? p.getDescription() : ""
                    ));
                }
                sb.append("Найдено: ").append(products.size());
                return sb.toString();
            }

            if (command.startsWith("GET_PRODUCT:")) {
                int id = Integer.parseInt(request.substring(12));
                Product p = dbManager.getProductById(id);
                if (p == null) return "Товар не найден.";
                return String.format("%d|%s|%d|%s|%s|%d|%.2f|%.2f|%d|%s",
                        p.getId(), p.getName(), p.getCategoryId(),
                        p.getCategoryName(), p.getBrand(), p.getVolumeMl(),
                        p.getPurchasePrice(), p.getRetailPrice(),
                        p.getStockQuantity(),
                        p.getDescription() != null ? p.getDescription() : "");
            }

            if (command.equals("SUPPLIERS_LIST")) {
                List<String[]> suppliers = dbManager.getAllSuppliers();
                StringBuilder sb = new StringBuilder();
                for (String[] s : suppliers) {
                    sb.append(s[0]).append("|").append(s[1]).append("|").append(s[2])
                            .append("|").append(s[3]).append("|").append(s[4]).append("|").append(s[5]).append("\n");
                }
                return sb.toString();
            }

            if (command.startsWith("ADD_SUPPLIER:")) {
                if (!user.isAdmin() && !user.isManager()) return "Ошибка: недостаточно прав.";
                String data = request.substring(13);
                String[] parts = data.split("\\|");
                if (parts.length < 5) return "Ошибка: неверный формат.";
                dbManager.addSupplier(parts[0], parts[1], parts[2], parts[3], parts[4]);
                return "Поставщик добавлен.";
            }

            if (command.startsWith("UPDATE_SUPPLIER:")) {
                String[] p = request.substring(16).split("\\|");
                dbManager.updateSupplier(Integer.parseInt(p[0]), p[1], p[2], p[3], p[4], p[5]);
                return "Поставщик обновлён.";
            }

            if (command.startsWith("DELETE_SUPPLIER:")) {
                if (!user.isAdmin()) return "Ошибка: требуется ADMIN.";
                dbManager.deactivateSupplier(Integer.parseInt(request.substring(16)));
                return "Поставщик деактивирован.";
            }

            if (command.startsWith("ADD_CLIENT:")) {
                String[] p = request.substring(11).split("\\|");
                dbManager.addClient(p[0], p[1], p[2], p[3]);
                return "Клиент добавлен.";
            }

            if (command.startsWith("UPDATE_CLIENT:")) {
                String[] p = request.substring(14).split("\\|");
                dbManager.updateClient(Integer.parseInt(p[0]), p[1], p[2], p[3], p[4]);
                return "Клиент обновлён.";
            }

            if (command.startsWith("DELETE_CLIENT:")) {
                dbManager.deactivateClient(Integer.parseInt(request.substring(14)));
                return "Клиент деактивирован.";
            }

            if (command.equals("USERS_LIST")) {
                if (!user.isAdmin()) return "Ошибка: требуется ADMIN.";
                List<String[]> users = dbManager.getAllUsersWithRoles();
                StringBuilder sb = new StringBuilder();
                for (String[] u : users) {
                    sb.append(u[0]).append("|").append(u[1]).append("|").append(u[2])
                            .append("|").append(u[3]).append("|").append(u[4]).append("\n");
                }
                return sb.toString();
            }

            if (command.startsWith("ADD_USER:")) {
                if (!user.isAdmin()) return "Ошибка: требуется ADMIN.";
                String[] p = request.substring(9).split("\\|");
                if (p.length < 4) return "Ошибка: формат ADD_USER:login|password|fullName|roleId";
                dbManager.addUser(p[0], p[1], p[2], Integer.parseInt(p[3]));
                return "Пользователь добавлен.";
            }

            if (command.startsWith("UPDATE_USER:")) {
                if (!user.isAdmin()) return "Ошибка: требуется ADMIN.";
                String[] p = request.substring(12).split("\\|");
                dbManager.updateUser(Integer.parseInt(p[0]), p[1], Integer.parseInt(p[2]),
                        p.length > 3 ? p[3] : null);
                return "Пользователь обновлён.";
            }

            if (command.startsWith("TOGGLE_USER:")) {
                if (!user.isAdmin()) return "Ошибка: требуется ADMIN.";
                dbManager.toggleUserStatus(Integer.parseInt(request.substring(12)));
                return "Статус изменён.";
            }

            if (command.startsWith("RETURN:")) {
                if (!user.isAdmin() && !user.isManager()) return "Ошибка: недостаточно прав.";
                String[] p = request.substring(7).split("\\|");
                dbManager.processReturn(Integer.parseInt(p[0]), Integer.parseInt(p[1]),
                        Integer.parseInt(p[2]), p.length > 3 ? p[3] : "");
                return "Возврат оформлен.";
            }

            if (command.startsWith("CHECK_RETURN:")) {
                String[] p = request.substring(13).split("\\|");
                int qty = Integer.parseInt(p[2]);
                if (qty <= 0) return "ERROR:Количество должно быть больше 0";
                return "OK";
            }

            if (command.startsWith("GET_RETURN_INFO:")) {
                int saleId = Integer.parseInt(request.substring(16));
                String info = dbManager.getReturnInfo(saleId);
                return info;
            }

            if (command.startsWith("GET_SALE_ITEMS:")) {
                int saleId = Integer.parseInt(request.substring(15));
                List<String[]> items = dbManager.getSaleItems(saleId);
                StringBuilder sb = new StringBuilder();
                for (String[] item : items) {
                    sb.append(item[0]).append("|").append(item[1]).append("|").append(item[2]).append("\n");
                }
                return sb.toString();
            }

            if (command.equals("CATEGORIES_FULL")) {
                List<String[]> cats = dbManager.getAllCategoriesFull();
                StringBuilder sb = new StringBuilder();
                for (String[] c : cats) sb.append(c[0]).append("|").append(c[1]).append("|").append(c[2]).append("\n");
                return sb.toString();
            }

            if (command.startsWith("ADD_CATEGORY:")) {
                if (!user.isAdmin()) return "Ошибка: требуется ADMIN.";
                String[] p = request.substring(13).split("\\|");
                dbManager.addCategory(p[0], p.length > 1 ? p[1] : "");
                return "Категория добавлена.";
            }

            if (command.startsWith("UPDATE_CATEGORY:")) {
                if (!user.isAdmin()) return "Ошибка: требуется ADMIN.";
                String[] p = request.substring(16).split("\\|");
                dbManager.updateCategory(Integer.parseInt(p[0]), p[1], p.length > 2 ? p[2] : "");
                return "Категория обновлена.";
            }

            if (command.startsWith("DELETE_CATEGORY:")) {
                if (!user.isAdmin()) return "Ошибка: требуется ADMIN.";
                try {
                    String idStr = request.substring(request.indexOf(":") + 1).trim();
                    System.out.println("DEBUG DELETE: idStr=[" + idStr + "]");
                    dbManager.deleteCategory(Integer.parseInt(idStr));
                    return "Категория удалена.";
                } catch (SQLException e) {
                    return "Ошибка: " + e.getMessage();
                } catch (NumberFormatException e) {
                    return "Ошибка: неверный ID категории";
                }
            }

            if (command.equals("EXIT")) {
                return "До свидания, " + user.getFullName() + "!";
            }

            return "Неизвестная команда. Введите HELP.";

        } catch (Exception e) {
            return "Ошибка: " + e.getMessage();
        }
    }

    private double parseDouble(String s) {
        return Double.parseDouble(s.replace(",", "."));
    }

    public static void main(String[] args) {
        new ServerV4().start();
    }
}