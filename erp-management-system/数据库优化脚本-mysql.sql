-- ERP 管理系统：MySQL 索引优化脚本
-- 使用 Navicat 或 mysql 客户端执行。脚本只创建独立的 SQL 实验表，
-- 不会修改主业务表。

CREATE DATABASE IF NOT EXISTS erp_training
  CHARACTER SET utf8mb4
  COLLATE utf8mb4_0900_ai_ci;

USE erp_training;

CREATE TABLE IF NOT EXISTS sql_lab_orders (
  id BIGINT NOT NULL PRIMARY KEY,
  partner_id BIGINT NOT NULL,
  business_date DATE NOT NULL,
  amount DECIMAL(18,2) NOT NULL,
  status VARCHAR(20) NOT NULL,
  KEY idx_lab_partner_date (partner_id, business_date)
) ENGINE=InnoDB;

-- 首次执行写入 20,000 条可重复生成的业务样本；重复执行不会产生重复主键。
SET SESSION cte_max_recursion_depth = 25000;
INSERT IGNORE INTO sql_lab_orders (id, partner_id, business_date, amount, status)
WITH RECURSIVE seq(n) AS (
  SELECT 1
  UNION ALL
  SELECT n + 1 FROM seq WHERE n < 20000
)
SELECT
  n,
  MOD(n, 400) + 1,
  DATE_SUB(CURRENT_DATE, INTERVAL MOD(n, 180) DAY),
  ROUND((MOD(n, 2000) + 100) / 100, 2),
  IF(MOD(n, 4) = 0, 'DRAFT', 'COMPLETED')
FROM seq;

-- 优化前：对索引列做类型转换，索引难以进行精确定位。
EXPLAIN
SELECT COUNT(*) AS row_count, SUM(amount) AS total_amount
FROM sql_lab_orders
WHERE CAST(partner_id AS CHAR) = '42';

-- 优化后：保持 partner_id 的数值类型，直接使用联合索引的第一列。
EXPLAIN
SELECT COUNT(*) AS row_count, SUM(amount) AS total_amount
FROM sql_lab_orders
WHERE partner_id = 42;

-- 两条查询的结果应保持一致；重点查看优化后的 key、type 和 rows。
SELECT 'before' AS query_version, COUNT(*) AS row_count, SUM(amount) AS total_amount
FROM sql_lab_orders
WHERE CAST(partner_id AS CHAR) = '42'
UNION ALL
SELECT 'after', COUNT(*), SUM(amount)
FROM sql_lab_orders
WHERE partner_id = 42;
