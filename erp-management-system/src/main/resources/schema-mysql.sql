CREATE TABLE IF NOT EXISTS partners (
  id BIGINT AUTO_INCREMENT PRIMARY KEY,
  name VARCHAR(100) NOT NULL,
  type VARCHAR(20) NOT NULL CHECK (type IN ('SUPPLIER','CUSTOMER')),
  contact VARCHAR(60) NOT NULL DEFAULT '',
  phone VARCHAR(40) NOT NULL DEFAULT '',
  created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
  UNIQUE KEY uk_partner_name_type(name,type)
);
CREATE TABLE IF NOT EXISTS products (
  id BIGINT AUTO_INCREMENT PRIMARY KEY,
  sku VARCHAR(40) NOT NULL UNIQUE,
  name VARCHAR(120) NOT NULL,
  category VARCHAR(40) NOT NULL,
  unit VARCHAR(20) NOT NULL,
  purchase_price DECIMAL(18,2) NOT NULL CHECK (purchase_price>=0),
  sale_price DECIMAL(18,2) NOT NULL CHECK (sale_price>=0),
  average_cost DECIMAL(18,6) NOT NULL DEFAULT 0 CHECK (average_cost>=0),
  stock DECIMAL(18,3) NOT NULL DEFAULT 0 CHECK (stock>=0),
  min_stock DECIMAL(18,3) NOT NULL DEFAULT 0 CHECK (min_stock>=0),
  created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
);
CREATE TABLE IF NOT EXISTS business_orders (
  id BIGINT AUTO_INCREMENT PRIMARY KEY,
  order_no VARCHAR(50) NOT NULL UNIQUE,
  type VARCHAR(20) NOT NULL CHECK (type IN ('PURCHASE','SALE')),
  partner_id BIGINT NOT NULL,
  status VARCHAR(20) NOT NULL CHECK (status IN ('DRAFT','COMPLETED','CANCELLED')),
  business_date DATE NOT NULL,
  total_amount DECIMAL(18,2) NOT NULL CHECK (total_amount>=0),
  note VARCHAR(500) NOT NULL DEFAULT '',
  created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
  completed_at TIMESTAMP NULL,
  FOREIGN KEY(partner_id) REFERENCES partners(id),
  KEY idx_orders_type_status_date(type,status,business_date)
);
CREATE TABLE IF NOT EXISTS order_lines (
  id BIGINT AUTO_INCREMENT PRIMARY KEY,
  order_id BIGINT NOT NULL,
  product_id BIGINT NOT NULL,
  quantity DECIMAL(18,3) NOT NULL CHECK (quantity>0),
  unit_price DECIMAL(18,2) NOT NULL CHECK (unit_price>=0),
  line_amount DECIMAL(18,2) NOT NULL CHECK (line_amount>=0),
  cost_amount DECIMAL(18,2) NOT NULL DEFAULT 0,
  FOREIGN KEY(order_id) REFERENCES business_orders(id),
  FOREIGN KEY(product_id) REFERENCES products(id),
  KEY idx_lines_order(order_id)
);
CREATE TABLE IF NOT EXISTS inventory_movements (
  id BIGINT AUTO_INCREMENT PRIMARY KEY,
  product_id BIGINT NOT NULL,
  order_id BIGINT NULL,
  type VARCHAR(20) NOT NULL CHECK (type IN ('OPENING','PURCHASE','SALE')),
  quantity DECIMAL(18,3) NOT NULL,
  balance DECIMAL(18,3) NOT NULL CHECK (balance>=0),
  unit_cost DECIMAL(18,6) NOT NULL,
  business_date DATE NOT NULL,
  created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
  FOREIGN KEY(product_id) REFERENCES products(id),
  FOREIGN KEY(order_id) REFERENCES business_orders(id),
  KEY idx_movements_product(product_id,id)
);
