-- ============================================
-- ПОЛНЫЙ СКРИПТ БАЗЫ ДАННЫХ
-- Парфюмерный магазин
-- Автор: [Чернякова Алина Сергеевна]
-- Дата: май 2026
-- ============================================

DROP DATABASE IF EXISTS parfumery_db;
CREATE DATABASE parfumery_db 
    CHARACTER SET utf8mb4 
    COLLATE utf8mb4_unicode_ci;

USE parfumery_db;

-- ============================================
-- 1. СПРАВОЧНИКИ
-- ============================================

-- Роли сотрудников
CREATE TABLE roles (
    id INT AUTO_INCREMENT PRIMARY KEY,
    name VARCHAR(50) NOT NULL UNIQUE,
    description VARCHAR(200)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- Сотрудники
CREATE TABLE users (
    id INT AUTO_INCREMENT PRIMARY KEY,
    login VARCHAR(50) NOT NULL UNIQUE,
    password VARCHAR(255) NOT NULL,
    full_name VARCHAR(100) NOT NULL,
    role_id INT NOT NULL,
    is_active BOOLEAN DEFAULT TRUE,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    FOREIGN KEY (role_id) REFERENCES roles(id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- Категории товаров
CREATE TABLE categories (
    id INT AUTO_INCREMENT PRIMARY KEY,
    name VARCHAR(100) NOT NULL UNIQUE,
    description TEXT
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- Товары
CREATE TABLE products (
    id INT AUTO_INCREMENT PRIMARY KEY,
    name VARCHAR(200) NOT NULL,
    category_id INT NOT NULL,
    brand VARCHAR(100),
    volume_ml INT,
    purchase_price DECIMAL(10,2),
    retail_price DECIMAL(10,2),
    stock_quantity INT DEFAULT 0,
    is_active BOOLEAN DEFAULT TRUE,
    description TEXT,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    FOREIGN KEY (category_id) REFERENCES categories(id) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- Клиенты
CREATE TABLE clients (
    id INT AUTO_INCREMENT PRIMARY KEY,
    name VARCHAR(100) NOT NULL,
    phone VARCHAR(20),
    email VARCHAR(100),
    address VARCHAR(200),
    is_active BOOLEAN DEFAULT TRUE,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- Поставщики
CREATE TABLE suppliers (
    id INT AUTO_INCREMENT PRIMARY KEY,
    name VARCHAR(150) NOT NULL,
    contact_person VARCHAR(100),
    phone VARCHAR(20),
    email VARCHAR(100),
    address VARCHAR(200),
    is_active BOOLEAN DEFAULT TRUE,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- ============================================
-- 2. ДВИЖЕНИЕ ТОВАРОВ
-- ============================================

-- Продажи (чеки)
CREATE TABLE sales (
    id INT AUTO_INCREMENT PRIMARY KEY,
    client_id INT,
    user_id INT NOT NULL,
    sale_date TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    total_amount DECIMAL(12,2) NOT NULL,
    FOREIGN KEY (client_id) REFERENCES clients(id),
    FOREIGN KEY (user_id) REFERENCES users(id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- Позиции в чеке
CREATE TABLE sales_items (
    id INT AUTO_INCREMENT PRIMARY KEY,
    sale_id INT NOT NULL,
    product_id INT NOT NULL,
    quantity INT NOT NULL CHECK (quantity > 0),
    price DECIMAL(10,2) NOT NULL,
    FOREIGN KEY (sale_id) REFERENCES sales(id) ON DELETE CASCADE,
    FOREIGN KEY (product_id) REFERENCES products(id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- Поставки
CREATE TABLE supplies (
    id INT AUTO_INCREMENT PRIMARY KEY,
    supplier_id INT,
    supplier_name VARCHAR(100),
    user_id INT NOT NULL,
    supply_date TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    total_amount DECIMAL(12,2) NOT NULL,
    FOREIGN KEY (supplier_id) REFERENCES suppliers(id),
    FOREIGN KEY (user_id) REFERENCES users(id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- Позиции в поставке
CREATE TABLE supplies_items (
    id INT AUTO_INCREMENT PRIMARY KEY,
    supply_id INT NOT NULL,
    product_id INT NOT NULL,
    quantity INT NOT NULL CHECK (quantity > 0),
    purchase_price DECIMAL(10,2) NOT NULL,
    FOREIGN KEY (supply_id) REFERENCES supplies(id) ON DELETE CASCADE,
    FOREIGN KEY (product_id) REFERENCES products(id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- Возвраты
CREATE TABLE returns (
    id INT AUTO_INCREMENT PRIMARY KEY,
    sale_id INT NOT NULL,
    product_id INT NOT NULL,
    quantity INT NOT NULL CHECK (quantity > 0),
    return_date TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    reason VARCHAR(200),
    FOREIGN KEY (sale_id) REFERENCES sales(id),
    FOREIGN KEY (product_id) REFERENCES products(id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- ============================================
-- 3. ТЕСТОВЫЕ ДАННЫЕ
-- ============================================

-- Роли
INSERT INTO roles (name, description) VALUES
('ADMIN', 'Администратор системы'),
('MANAGER', 'Менеджер по продажам'),
('ANALYST', 'Аналитик');

-- Сотрудники
INSERT INTO users (login, password, full_name, role_id) VALUES
('admin', 'admin123', 'Иванова Анна Петровна', 1),
('manager', 'manager123', 'Смирнов Дмитрий Сергеевич', 2),
('analyst', 'analyst123', 'Петрова Елена Викторовна', 3);

-- Категории
INSERT INTO categories (name, description) VALUES
('Женская парфюмерия', 'Духи и туалетная вода для женщин'),
('Мужская парфюмерия', 'Духи и туалетная вода для мужчин'),
('Унисекс', 'Парфюмерия без гендерной привязки'),
('Нишевая парфюмерия', 'Эксклюзивные и редкие ароматы'),
('Пробники и миниатюры', 'Маленькие объёмы для знакомства с ароматом');

-- Товары
INSERT INTO products (name, category_id, brand, volume_ml, purchase_price, retail_price, stock_quantity, description) VALUES
('Chanel No. 5', 1, 'Chanel', 50, 4500.00, 8900.00, 11, 'Легендарный женский аромат'),
('J''adore', 1, 'Dior', 50, 3800.00, 7500.00, 12, 'Цветочный женский аромат'),
('Sauvage', 2, 'Dior', 100, 4200.00, 8200.00, 19, 'Свежий мужской аромат'),
('Bleu de Chanel', 2, 'Chanel', 100, 5000.00, 9500.00, 15, 'Древесный мужской аромат'),
('Acqua di Gio', 3, 'Giorgio Armani', 75, 3500.00, 6800.00, 9, 'Морской унисекс-аромат'),
('Baccarat Rouge 540', 4, 'Maison Francis Kurkdjian', 70, 12000.00, 24000.00, 10, 'Культовый нишевый аромат'),
('Black Opium', 1, 'Yves Saint Laurent', 90, 4100.00, 7900.00, 17, 'Восточный женский аромат'),
('Le Male', 2, 'Jean Paul Gaultier', 75, 3600.00, 7000.00, 14, 'Классический мужской аромат');

-- Клиенты
INSERT INTO clients (name, phone, email, address) VALUES
('Козлова Мария Игоревна', '+375291234567', 'm.kozlova@mail.ru', 'г. Минск, ул. Пушкина, 15-42'),
('Новиков Андрей Валерьевич', '+375292345678', 'novikov@mail.ru', 'г. Минск, пр. Независимости, 100-12'),
('Морозова Татьяна Сергеевна', '+375293456789', 'morozova@mail.ru', 'г. Минск, ул. Якуба Коласа, 50-3'),
('Волков Сергей Александрович', '+375294567890', 'volkov@gmail.com', 'г. Минск, ул. Сурганова, 20-55'),
('Соколова Анна Дмитриевна', '+375295678901', 'sokolova@mail.ru', 'г. Минск, пр. Победителей, 75-88'),
('Зайцев Игорь Павлович', '+375296789012', 'zaytsev@gmail.com', 'г. Минск, ул. Кальварийская, 30-17');

-- Поставщики
INSERT INTO suppliers (name, contact_person, phone, email, address) VALUES
('ООО "Парфюм-Трейд"', 'Иванов Сергей Петрович', '+375291112233', 'parfum-trade@mail.ru', 'г. Минск, ул. Ленина, 10'),
('ИП "Аромат-Маркет"', 'Петрова Анна Игоревна', '+375292223344', 'aromat-market@mail.ru', 'г. Минск, пр. Мира, 25'),
('ЗАО "Бьюти-Групп"', 'Сидоров Дмитрий Валерьевич', '+375293334455', 'beauty-group@gmail.com', 'г. Минск, ул. Садовая, 7');

-- Продажи (разные даты для демонстрации фильтров)
INSERT INTO sales (client_id, user_id, sale_date, total_amount) VALUES
(1, 2, DATE_SUB(NOW(), INTERVAL 1 DAY), 16400.00),
(2, 2, DATE_SUB(NOW(), INTERVAL 1 DAY), 8200.00),
(3, 2, DATE_SUB(NOW(), INTERVAL 1 DAY), 24000.00),
(4, 2, DATE_SUB(NOW(), INTERVAL 5 DAY), 7500.00),
(NULL, 2, DATE_SUB(NOW(), INTERVAL 5 DAY), 8900.00),
(5, 2, DATE_SUB(NOW(), INTERVAL 10 DAY), 15700.00),
(6, 2, DATE_SUB(NOW(), INTERVAL 10 DAY), 8200.00),
(1, 2, DATE_SUB(NOW(), INTERVAL 25 DAY), 6800.00),
(NULL, 2, DATE_SUB(NOW(), INTERVAL 25 DAY), 9500.00),
(3, 2, DATE_SUB(NOW(), INTERVAL 25 DAY), 14900.00),
(NULL, 1, DATE_SUB(NOW(), INTERVAL 35 DAY), 8900.00),
(1, 1, DATE_SUB(NOW(), INTERVAL 35 DAY), 26000.00),
(4, 1, DATE_SUB(NOW(), INTERVAL 2 DAY), 16800.00),
(1, 1, NOW(), 6800.00);

-- Позиции в продажах
INSERT INTO sales_items (sale_id, product_id, quantity, price) VALUES
(1, 1, 1, 8900.00), (1, 2, 1, 7500.00),
(2, 3, 1, 8200.00),
(3, 6, 1, 24000.00),
(4, 2, 1, 7500.00),
(5, 1, 1, 8900.00),
(6, 7, 1, 7900.00), (6, 8, 1, 7800.00),
(7, 3, 1, 8200.00),
(8, 5, 1, 6800.00),
(9, 4, 1, 9500.00),
(10, 1, 1, 8900.00), (10, 3, 1, 6000.00),
(11, 1, 1, 8900.00),
(12, 1, 2, 8900.00), (12, 3, 1, 8200.00),
(13, 1, 1, 8900.00), (13, 7, 1, 7900.00),
(14, 5, 1, 6800.00);

-- Поставки
INSERT INTO supplies (supplier_id, supplier_name, user_id, supply_date, total_amount) VALUES
(1, 'ООО "Парфюм-Трейд"', 1, DATE_SUB(NOW(), INTERVAL 20 DAY), 83500.00),
(2, 'ИП "Аромат-Маркет"', 1, DATE_SUB(NOW(), INTERVAL 15 DAY), 45500.00),
(1, 'ООО "Парфюм-Трейд"', 1, DATE_SUB(NOW(), INTERVAL 5 DAY), 85000.00);

-- Позиции в поставках
INSERT INTO supplies_items (supply_id, product_id, quantity, purchase_price) VALUES
(1, 1, 5, 4500.00), (1, 3, 10, 4200.00),
(2, 6, 2, 12000.00), (2, 2, 5, 3800.00),
(3, 4, 4, 5000.00), (3, 7, 6, 4100.00), (3, 5, 3, 3500.00);

-- Возвраты (пара примеров)
INSERT INTO returns (sale_id, product_id, quantity, reason) VALUES
(1, 2, 1, 'Не подошёл аромат'),
(6, 8, 1, 'Брак упаковки');