package parfumery;

import java.io.*;
import java.net.*;
import java.util.Scanner;

/**
 * Клиент версии 3 — с авторизацией и бизнес-командами.
 */
public class ClientV3 {

    private static final String SERVER_ADDRESS = "localhost";
    private static final int SERVER_PORT = 8080;

    public static void main(String[] args) {
        System.out.println("=== Клиент парфюмерного магазина v3 ===");
        System.out.println("Подключаюсь к серверу...");

        try (Socket socket = new Socket(SERVER_ADDRESS, SERVER_PORT);
             BufferedReader in = new BufferedReader(
                     new InputStreamReader(socket.getInputStream(), "UTF-8"));
             BufferedWriter out = new BufferedWriter(
                     new OutputStreamWriter(socket.getOutputStream(), "UTF-8"));
             Scanner scanner = new Scanner(System.in, "UTF-8")) {

            System.out.println("Подключено!");

            // === Фаза 1: Авторизация ===
            System.out.println("Авторизуйтесь: введите LOGIN:логин:пароль");
            System.out.println("  Тестовые: admin:admin123, manager:manager123, analyst:analyst123");

            boolean authorized = false;
            while (!authorized) {
                System.out.print("> ");
                String input = scanner.nextLine();

                out.write(input);
                out.newLine();
                out.flush();

                String response = in.readLine();

                if (response.startsWith("OK:")) {
                    String[] parts = response.split(":");
                    System.out.println("Добро пожаловать, " + parts[2] + "!");
                    System.out.println("Ваша роль: " + parts[1]);
                    authorized = true;
                } else if (response.startsWith("ERROR:")) {
                    System.out.println("Ошибка: " + response.substring(6));
                } else if (response.equals("BYE")) {
                    System.out.println("До свидания!");
                    return;
                }
            }

            // Ждём сигнал READY
            String ready = in.readLine();

            // === Фаза 2: Работа с командами ===
            System.out.println("Введите HELP для списка команд.");
            System.out.println("Примеры:");
            System.out.println("  LIST, CLIENTS, SALES_HISTORY");
            System.out.println("  SALE:0:1:1:8900,3:2:8200    (продажа: без клиента, товар1 x1, товар3 x2)");
            System.out.println("  SUPPLY:Поставщик:1:5:4500   (поставка: товар1 x5 по 4500)");

            while (true) {
                System.out.print("> ");
                String input = scanner.nextLine();

                if (input.trim().isEmpty()) continue;

                out.write(input);
                out.newLine();
                out.flush();

                if (input.trim().equalsIgnoreCase("EXIT")) {
                    String line;
                    while ((line = in.readLine()) != null) {
                        if (line.equals("===END===")) break;
                        System.out.println(line);
                    }
                    break;
                }

                System.out.println("--- Ответ сервера ---");
                String line;
                while ((line = in.readLine()) != null) {
                    if (line.equals("===END===")) break;
                    System.out.println(line);
                }
                System.out.println();
            }

        } catch (ConnectException e) {
            System.err.println("Не удалось подключиться. Сервер запущен?");
        } catch (IOException e) {
            System.err.println("Ошибка клиента: " + e.getMessage());
        }

        System.out.println("Клиент остановлен.");
    }
}