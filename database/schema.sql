-- ============================================================
-- SPIRITLANE - Alcohol Home Delivery Platform
-- Database Schema | MySQL 8.0+
-- ============================================================

CREATE DATABASE IF NOT EXISTS spiritlane_db CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;
USE spiritlane_db;

-- ============================================================
-- TABLE: roles
-- ============================================================
CREATE TABLE roles (
    id          BIGINT AUTO_INCREMENT PRIMARY KEY,
    name        VARCHAR(50) NOT NULL UNIQUE,
    description VARCHAR(255),
    created_at  TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

-- ============================================================
-- TABLE: users  (All roles share this table)
-- ============================================================
CREATE TABLE users (
    id               BIGINT AUTO_INCREMENT PRIMARY KEY,
    full_name        VARCHAR(150) NOT NULL,
    email            VARCHAR(150) NOT NULL UNIQUE,
    phone            VARCHAR(15)  NOT NULL UNIQUE,
    password_hash    VARCHAR(255) NOT NULL,
    role_id          BIGINT       NOT NULL,
    date_of_birth    DATE         NOT NULL,         -- 18+ verification
    is_age_verified  BOOLEAN      DEFAULT FALSE,
    profile_image    VARCHAR(500),
    is_active        BOOLEAN      DEFAULT TRUE,
    is_blocked       BOOLEAN      DEFAULT FALSE,
    created_at       TIMESTAMP    DEFAULT CURRENT_TIMESTAMP,
    updated_at       TIMESTAMP    DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    CONSTRAINT fk_user_role FOREIGN KEY (role_id) REFERENCES roles(id)
);

-- ============================================================
-- TABLE: user_addresses
-- ============================================================
CREATE TABLE user_addresses (
    id           BIGINT AUTO_INCREMENT PRIMARY KEY,
    user_id      BIGINT       NOT NULL,
    address_line1 VARCHAR(255) NOT NULL,
    address_line2 VARCHAR(255),
    city         VARCHAR(100) NOT NULL,
    state        VARCHAR(100) NOT NULL,
    pincode      VARCHAR(10)  NOT NULL,
    landmark     VARCHAR(255),
    address_type ENUM('HOME','OFFICE','OTHER') DEFAULT 'HOME',
    is_default   BOOLEAN      DEFAULT FALSE,
    created_at   TIMESTAMP    DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT fk_addr_user FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE CASCADE
);

-- ============================================================
-- TABLE: shops
-- ============================================================
CREATE TABLE shops (
    id               BIGINT AUTO_INCREMENT PRIMARY KEY,
    owner_id         BIGINT       NOT NULL,
    shop_name        VARCHAR(200) NOT NULL,
    license_number   VARCHAR(100) NOT NULL UNIQUE,   -- liquor license
    gst_number       VARCHAR(20),
    description      TEXT,
    shop_image       VARCHAR(500),
    address_line1    VARCHAR(255) NOT NULL,
    address_line2    VARCHAR(255),
    city             VARCHAR(100) NOT NULL,
    state            VARCHAR(100) NOT NULL,
    pincode          VARCHAR(10)  NOT NULL,
    latitude         DECIMAL(10,8),
    longitude        DECIMAL(11,8),
    phone            VARCHAR(15)  NOT NULL,
    email            VARCHAR(150),
    opening_time     TIME         DEFAULT '10:00:00',
    closing_time     TIME         DEFAULT '22:00:00',
    is_open          BOOLEAN      DEFAULT TRUE,
    is_approved      BOOLEAN      DEFAULT FALSE,
    is_active        BOOLEAN      DEFAULT TRUE,
    rating           DECIMAL(3,2) DEFAULT 0.00,
    total_ratings    INT          DEFAULT 0,
    created_at       TIMESTAMP    DEFAULT CURRENT_TIMESTAMP,
    updated_at       TIMESTAMP    DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    CONSTRAINT fk_shop_owner FOREIGN KEY (owner_id) REFERENCES users(id)
);

-- ============================================================
-- TABLE: categories
-- ============================================================
CREATE TABLE categories (
    id          BIGINT AUTO_INCREMENT PRIMARY KEY,
    name        VARCHAR(100) NOT NULL UNIQUE,
    slug        VARCHAR(100) NOT NULL UNIQUE,
    description TEXT,
    image_url   VARCHAR(500),
    icon_class  VARCHAR(100),
    is_active   BOOLEAN      DEFAULT TRUE,
    sort_order  INT          DEFAULT 0,
    created_at  TIMESTAMP    DEFAULT CURRENT_TIMESTAMP
);

-- ============================================================
-- TABLE: brands
-- ============================================================
CREATE TABLE brands (
    id          BIGINT AUTO_INCREMENT PRIMARY KEY,
    name        VARCHAR(150) NOT NULL UNIQUE,
    slug        VARCHAR(150) NOT NULL UNIQUE,
    logo_url    VARCHAR(500),
    country     VARCHAR(100),
    description TEXT,
    is_active   BOOLEAN      DEFAULT TRUE,
    created_at  TIMESTAMP    DEFAULT CURRENT_TIMESTAMP
);

-- ============================================================
-- TABLE: products
-- ============================================================
CREATE TABLE products (
    id               BIGINT AUTO_INCREMENT PRIMARY KEY,
    name             VARCHAR(250) NOT NULL,
    slug             VARCHAR(250) NOT NULL UNIQUE,
    description      TEXT,
    category_id      BIGINT       NOT NULL,
    brand_id         BIGINT,
    alcohol_content  DECIMAL(5,2),                   -- ABV %
    volume_ml        INT,                              -- bottle size in ml
    country_of_origin VARCHAR(100),
    product_type     ENUM('BEER','WINE','WHISKEY','VODKA','RUM','GIN','TEQUILA','BRANDY','COCKTAIL','OTHER') NOT NULL,
    is_imported      BOOLEAN      DEFAULT FALSE,
    tags             VARCHAR(500),                    -- comma-separated tags
    is_active        BOOLEAN      DEFAULT TRUE,
    created_at       TIMESTAMP    DEFAULT CURRENT_TIMESTAMP,
    updated_at       TIMESTAMP    DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    CONSTRAINT fk_product_category FOREIGN KEY (category_id) REFERENCES categories(id),
    CONSTRAINT fk_product_brand    FOREIGN KEY (brand_id)    REFERENCES brands(id)
);

-- ============================================================
-- TABLE: product_images
-- ============================================================
CREATE TABLE product_images (
    id          BIGINT AUTO_INCREMENT PRIMARY KEY,
    product_id  BIGINT       NOT NULL,
    image_url   VARCHAR(500) NOT NULL,
    alt_text    VARCHAR(255),
    is_primary  BOOLEAN      DEFAULT FALSE,
    sort_order  INT          DEFAULT 0,
    CONSTRAINT fk_img_product FOREIGN KEY (product_id) REFERENCES products(id) ON DELETE CASCADE
);

-- ============================================================
-- TABLE: shop_inventory  (Products listed by each shop with price & stock)
-- ============================================================
CREATE TABLE shop_inventory (
    id              BIGINT AUTO_INCREMENT PRIMARY KEY,
    shop_id         BIGINT         NOT NULL,
    product_id      BIGINT         NOT NULL,
    mrp             DECIMAL(10,2)  NOT NULL,
    selling_price   DECIMAL(10,2)  NOT NULL,
    discount_pct    DECIMAL(5,2)   DEFAULT 0.00,
    stock_quantity  INT            NOT NULL DEFAULT 0,
    min_order_qty   INT            DEFAULT 1,
    max_order_qty   INT            DEFAULT 10,
    is_available    BOOLEAN        DEFAULT TRUE,
    created_at      TIMESTAMP      DEFAULT CURRENT_TIMESTAMP,
    updated_at      TIMESTAMP      DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    UNIQUE KEY uq_shop_product (shop_id, product_id),
    CONSTRAINT fk_inv_shop    FOREIGN KEY (shop_id)    REFERENCES shops(id) ON DELETE CASCADE,
    CONSTRAINT fk_inv_product FOREIGN KEY (product_id) REFERENCES products(id)
);

-- ============================================================
-- TABLE: carts
-- ============================================================
CREATE TABLE carts (
    id         BIGINT AUTO_INCREMENT PRIMARY KEY,
    user_id    BIGINT    NOT NULL UNIQUE,
    shop_id    BIGINT,                                -- cart locked to one shop
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    CONSTRAINT fk_cart_user FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE CASCADE,
    CONSTRAINT fk_cart_shop FOREIGN KEY (shop_id) REFERENCES shops(id)
);

-- ============================================================
-- TABLE: cart_items
-- ============================================================
CREATE TABLE cart_items (
    id           BIGINT         AUTO_INCREMENT PRIMARY KEY,
    cart_id      BIGINT         NOT NULL,
    inventory_id BIGINT         NOT NULL,
    quantity     INT            NOT NULL DEFAULT 1,
    unit_price   DECIMAL(10,2)  NOT NULL,
    created_at   TIMESTAMP      DEFAULT CURRENT_TIMESTAMP,
    updated_at   TIMESTAMP      DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    CONSTRAINT fk_cartitem_cart      FOREIGN KEY (cart_id)      REFERENCES carts(id) ON DELETE CASCADE,
    CONSTRAINT fk_cartitem_inventory FOREIGN KEY (inventory_id) REFERENCES shop_inventory(id)
);

-- ============================================================
-- TABLE: orders
-- ============================================================
CREATE TABLE orders (
    id                  BIGINT         AUTO_INCREMENT PRIMARY KEY,
    order_number        VARCHAR(50)    NOT NULL UNIQUE,
    customer_id         BIGINT         NOT NULL,
    shop_id             BIGINT         NOT NULL,
    delivery_agent_id   BIGINT,
    delivery_address_id BIGINT         NOT NULL,
    subtotal            DECIMAL(10,2)  NOT NULL,
    delivery_charge     DECIMAL(10,2)  DEFAULT 0.00,
    discount_amount     DECIMAL(10,2)  DEFAULT 0.00,
    tax_amount          DECIMAL(10,2)  DEFAULT 0.00,
    total_amount        DECIMAL(10,2)  NOT NULL,
    order_status        ENUM('PENDING','CONFIRMED','PREPARING','DISPATCHED','OUT_FOR_DELIVERY','DELIVERED','CANCELLED','REJECTED') DEFAULT 'PENDING',
    payment_status      ENUM('PENDING','PAID','FAILED','REFUNDED') DEFAULT 'PENDING',
    notes               TEXT,
    estimated_delivery  TIMESTAMP,
    actual_delivery     TIMESTAMP,
    cancelled_reason    VARCHAR(500),
    created_at          TIMESTAMP      DEFAULT CURRENT_TIMESTAMP,
    updated_at          TIMESTAMP      DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    CONSTRAINT fk_order_customer   FOREIGN KEY (customer_id)       REFERENCES users(id),
    CONSTRAINT fk_order_shop       FOREIGN KEY (shop_id)           REFERENCES shops(id),
    CONSTRAINT fk_order_agent      FOREIGN KEY (delivery_agent_id) REFERENCES users(id),
    CONSTRAINT fk_order_address    FOREIGN KEY (delivery_address_id) REFERENCES user_addresses(id)
);

-- ============================================================
-- TABLE: order_items
-- ============================================================
CREATE TABLE order_items (
    id           BIGINT         AUTO_INCREMENT PRIMARY KEY,
    order_id     BIGINT         NOT NULL,
    inventory_id BIGINT         NOT NULL,
    product_name VARCHAR(250)   NOT NULL,            -- snapshot at order time
    quantity     INT            NOT NULL,
    unit_price   DECIMAL(10,2)  NOT NULL,
    total_price  DECIMAL(10,2)  NOT NULL,
    CONSTRAINT fk_oi_order     FOREIGN KEY (order_id)     REFERENCES orders(id) ON DELETE CASCADE,
    CONSTRAINT fk_oi_inventory FOREIGN KEY (inventory_id) REFERENCES shop_inventory(id)
);

-- ============================================================
-- TABLE: payments
-- ============================================================
CREATE TABLE payments (
    id                   BIGINT         AUTO_INCREMENT PRIMARY KEY,
    order_id             BIGINT         NOT NULL UNIQUE,
    razorpay_order_id    VARCHAR(100)   NOT NULL,
    razorpay_payment_id  VARCHAR(100),
    razorpay_signature   VARCHAR(500),
    amount               DECIMAL(10,2)  NOT NULL,
    currency             VARCHAR(10)    DEFAULT 'INR',
    payment_method       VARCHAR(50),                -- upi, card, netbanking
    status               ENUM('CREATED','PAID','FAILED','REFUNDED') DEFAULT 'CREATED',
    failure_reason       VARCHAR(500),
    refund_id            VARCHAR(100),
    paid_at              TIMESTAMP,
    created_at           TIMESTAMP      DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT fk_payment_order FOREIGN KEY (order_id) REFERENCES orders(id)
);

-- ============================================================
-- TABLE: coupons
-- ============================================================
CREATE TABLE coupons (
    id               BIGINT         AUTO_INCREMENT PRIMARY KEY,
    code             VARCHAR(50)    NOT NULL UNIQUE,
    description      VARCHAR(255),
    discount_type    ENUM('PERCENTAGE','FLAT') NOT NULL,
    discount_value   DECIMAL(10,2)  NOT NULL,
    min_order_amount DECIMAL(10,2)  DEFAULT 0.00,
    max_discount     DECIMAL(10,2),
    usage_limit      INT            DEFAULT 1,
    used_count       INT            DEFAULT 0,
    valid_from       TIMESTAMP,
    valid_until      TIMESTAMP,
    is_active        BOOLEAN        DEFAULT TRUE,
    created_at       TIMESTAMP      DEFAULT CURRENT_TIMESTAMP
);

-- ============================================================
-- TABLE: order_coupons
-- ============================================================
CREATE TABLE order_coupons (
    id          BIGINT        AUTO_INCREMENT PRIMARY KEY,
    order_id    BIGINT        NOT NULL,
    coupon_id   BIGINT        NOT NULL,
    discount    DECIMAL(10,2) NOT NULL,
    CONSTRAINT fk_oc_order  FOREIGN KEY (order_id)  REFERENCES orders(id),
    CONSTRAINT fk_oc_coupon FOREIGN KEY (coupon_id) REFERENCES coupons(id)
);

-- ============================================================
-- TABLE: reviews
-- ============================================================
CREATE TABLE reviews (
    id          BIGINT AUTO_INCREMENT PRIMARY KEY,
    user_id     BIGINT NOT NULL,
    product_id  BIGINT NOT NULL,
    order_id    BIGINT NOT NULL,
    rating      INT    NOT NULL CHECK (rating BETWEEN 1 AND 5),
    title       VARCHAR(255),
    comment     TEXT,
    is_approved BOOLEAN   DEFAULT TRUE,
    created_at  TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    UNIQUE KEY uq_user_product_order (user_id, product_id, order_id),
    CONSTRAINT fk_review_user    FOREIGN KEY (user_id)    REFERENCES users(id),
    CONSTRAINT fk_review_product FOREIGN KEY (product_id) REFERENCES products(id),
    CONSTRAINT fk_review_order   FOREIGN KEY (order_id)   REFERENCES orders(id)
);

-- ============================================================
-- TABLE: wishlists
-- ============================================================
CREATE TABLE wishlists (
    id         BIGINT    AUTO_INCREMENT PRIMARY KEY,
    user_id    BIGINT    NOT NULL,
    product_id BIGINT    NOT NULL,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    UNIQUE KEY uq_user_product (user_id, product_id),
    CONSTRAINT fk_wl_user    FOREIGN KEY (user_id)    REFERENCES users(id) ON DELETE CASCADE,
    CONSTRAINT fk_wl_product FOREIGN KEY (product_id) REFERENCES products(id)
);

-- ============================================================
-- TABLE: notifications
-- ============================================================
CREATE TABLE notifications (
    id         BIGINT AUTO_INCREMENT PRIMARY KEY,
    user_id    BIGINT       NOT NULL,
    title      VARCHAR(255) NOT NULL,
    message    TEXT         NOT NULL,
    type       ENUM('ORDER','PAYMENT','PROMO','SYSTEM') DEFAULT 'SYSTEM',
    is_read    BOOLEAN      DEFAULT FALSE,
    created_at TIMESTAMP    DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT fk_notif_user FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE CASCADE
);

-- ============================================================
-- TABLE: delivery_agent_details
-- ============================================================
CREATE TABLE delivery_agent_details (
    id              BIGINT AUTO_INCREMENT PRIMARY KEY,
    agent_id        BIGINT       NOT NULL UNIQUE,
    vehicle_type    ENUM('BIKE','SCOOTER','CAR') DEFAULT 'BIKE',
    vehicle_number  VARCHAR(20),
    license_number  VARCHAR(50),
    is_available    BOOLEAN      DEFAULT TRUE,
    current_lat     DECIMAL(10,8),
    current_lng     DECIMAL(11,8),
    total_deliveries INT         DEFAULT 0,
    rating          DECIMAL(3,2) DEFAULT 0.00,
    CONSTRAINT fk_agent_user FOREIGN KEY (agent_id) REFERENCES users(id)
);

-- ============================================================
-- TABLE: banners
-- ============================================================
CREATE TABLE banners (
    id          BIGINT AUTO_INCREMENT PRIMARY KEY,
    title       VARCHAR(200) NOT NULL,
    image_url   VARCHAR(500) NOT NULL,
    link_url    VARCHAR(500),
    position    ENUM('HOME_TOP','HOME_MID','CATEGORY') DEFAULT 'HOME_TOP',
    is_active   BOOLEAN      DEFAULT TRUE,
    sort_order  INT          DEFAULT 0,
    valid_from  TIMESTAMP,
    valid_until TIMESTAMP,
    created_at  TIMESTAMP    DEFAULT CURRENT_TIMESTAMP
);

-- ============================================================
-- SEED DATA
-- ============================================================

-- Roles
INSERT INTO roles (name, description) VALUES
('ROLE_ADMIN',          'Platform Administrator'),
('ROLE_CUSTOMER',       'End Customer / Buyer'),
('ROLE_SHOP_OWNER',     'Liquor Shop Owner / Seller'),
('ROLE_DELIVERY_AGENT', 'Delivery Agent');

-- Categories
INSERT INTO categories (name, slug, description, icon_class, sort_order) VALUES
('Beer',    'beer',    'Lagers, ales, stouts, and craft beers', 'fas fa-beer',         1),
('Wine',    'wine',    'Red, white, rosé, and sparkling wines', 'fas fa-wine-glass',   2),
('Whiskey', 'whiskey', 'Single malts, blends, and bourbon',     'fas fa-whiskey-glass',3),
('Vodka',   'vodka',   'Premium and flavoured vodkas',           'fas fa-glass-martini',4),
('Rum',     'rum',     'Dark, white, and spiced rums',           'fas fa-cocktail',     5),
('Gin',     'gin',     'London dry, flavoured, and craft gins',  'fas fa-leaf',         6),
('Tequila', 'tequila', 'Blanco, reposado, and añejo tequilas',   'fas fa-pepper-hot',   7),
('Brandy',  'brandy',  'VSOP, XO, and fruit brandies',           'fas fa-glass-whiskey',8),
('Cocktail Mix','cocktail-mix','Ready-to-drink cocktails and mixers','fas fa-blender',  9),
('Others',  'others',  'Other spirits and specialties',          'fas fa-ellipsis-h',  10);

-- Brands
INSERT INTO brands (name, slug, country) VALUES
('Kingfisher',  'kingfisher',  'India'),
('Royal Stag',  'royal-stag',  'India'),
('Old Monk',    'old-monk',    'India'),
('Absolut',     'absolut',     'Sweden'),
('Jack Daniels','jack-daniels','USA'),
('Johnnie Walker','johnnie-walker','Scotland'),
('Budweiser',   'budweiser',   'USA'),
('Corona',      'corona',      'Mexico'),
('Bacardi',     'bacardi',     'Bermuda'),
('Sula',        'sula',        'India');

-- Admin user (password: Admin@123 bcrypt)
INSERT INTO users (full_name, email, phone, password_hash, role_id, date_of_birth, is_age_verified, is_active)
VALUES ('Platform Admin', 'admin@spiritlane.in', '9999999999',
        '$2a$12$LQv3c1yqBWVHxkd0LHAkCOYz6TtxMQyCBNh6Bm8G7JKF9iCvXcBpS', 1, '1985-01-01', TRUE, TRUE);

-- ============================================================
-- INDEXES for performance
-- ============================================================
CREATE INDEX idx_users_email      ON users(email);
CREATE INDEX idx_users_phone      ON users(phone);
CREATE INDEX idx_users_role       ON users(role_id);
CREATE INDEX idx_products_cat     ON products(category_id);
CREATE INDEX idx_products_brand   ON products(brand_id);
CREATE INDEX idx_products_type    ON products(product_type);
CREATE INDEX idx_inventory_shop   ON shop_inventory(shop_id);
CREATE INDEX idx_inventory_prod   ON shop_inventory(product_id);
CREATE INDEX idx_orders_customer  ON orders(customer_id);
CREATE INDEX idx_orders_shop      ON orders(shop_id);
CREATE INDEX idx_orders_status    ON orders(order_status);
CREATE INDEX idx_orders_payment   ON orders(payment_status);
CREATE INDEX idx_reviews_product  ON reviews(product_id);
CREATE INDEX idx_notifications_user ON notifications(user_id, is_read);
