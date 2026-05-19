package parfumery;

import java.io.FileInputStream;
import java.io.IOException;
import java.util.Properties;

public class DatabaseConfig {

    private static final Properties props = new Properties();

    static {
        try {
            FileInputStream fis = new FileInputStream("config.properties");
            props.load(fis);
            fis.close();
            System.out.println("Конфигурация загружена из config.properties");
        } catch (IOException e) {
            System.err.println("Не удалось загрузить config.properties: " + e.getMessage());
            System.err.println("Используются настройки по умолчанию.");
        }
    }

    public static String getDbHost() {
        return props.getProperty("db.host", "localhost");
    }

    public static String getDbPort() {
        return props.getProperty("db.port", "3306");
    }

    public static String getDbName() {
        return props.getProperty("db.name", "parfumery_db");
    }

    public static String getDbUser() {
        return props.getProperty("db.user", "root");
    }

    public static String getDbPassword() {
        return props.getProperty("db.password", "root");
    }

    public static int getServerPort() {
        return Integer.parseInt(props.getProperty("server.port", "8080"));
    }

    public static int getServerThreads() {
        return Integer.parseInt(props.getProperty("server.threads", "10"));
    }

    public static String getConnectionUrl() {
        return "jdbc:mysql://" + getDbHost() + ":" + getDbPort() + "/" + getDbName()
                + "?useUnicode=true"
                + "&characterEncoding=UTF-8"
                + "&characterSetResults=UTF-8"
                + "&connectionCollation=utf8mb4_unicode_ci"
                + "&useSSL=false"
                + "&allowPublicKeyRetrieval=true"
                + "&serverTimezone=Europe/Minsk";
    }
}