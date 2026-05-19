package parfumery;

import java.io.*;
import java.util.List;

public class DataExporter {

    public static void saveProducts(List<Product> products, String filename) {
        try (ObjectOutputStream oos = new ObjectOutputStream(
                new FileOutputStream(filename))) {
            oos.writeObject(products);
            System.out.println("Товары сохранены в файл: " + filename);
            System.out.println("Сохранено товаров: " + products.size());
        } catch (IOException e) {
            System.err.println("Ошибка сохранения: " + e.getMessage());
        }
    }

    @SuppressWarnings("unchecked")
    public static List<Product> loadProducts(String filename) {
        try (ObjectInputStream ois = new ObjectInputStream(
                new FileInputStream(filename))) {
            List<Product> products = (List<Product>) ois.readObject();
            System.out.println("Товары загружены из файла: " + filename);
            System.out.println("Загружено товаров: " + products.size());
            return products;
        } catch (IOException | ClassNotFoundException e) {
            System.err.println("Ошибка загрузки: " + e.getMessage());
            return null;
        }
    }

    public static void printBackupInfo(String filename) {
        List<Product> products = loadProducts(filename);
        if (products != null) {
            System.out.println("=== Содержимое резервной копии ===");
            for (Product p : products) {
                System.out.println(p.toString());
            }
        }
    }
}