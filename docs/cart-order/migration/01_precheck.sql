/* 六張正式表 migration 執行前唯讀檢查；本檔不修改資料。 */
SET NOCOUNT ON;

SELECT DB_NAME() AS database_name, SCHEMA_NAME() AS default_schema;

IF DB_NAME() <> N'giga_fix'
    THROW 51000, '必須在 giga_fix 資料庫執行。', 1;

SELECT t.name AS table_name, SUM(p.rows) AS row_count
FROM sys.tables t
JOIN sys.schemas s ON s.schema_id = t.schema_id
LEFT JOIN sys.partitions p ON p.object_id = t.object_id AND p.index_id IN (0, 1)
WHERE s.name = N'dbo'
  AND t.name IN (N'carts', N'cart_items', N'orders', N'order_items',
                 N'cart', N'cart_item', N'order_item', N'payment', N'shipment',
                 N'members', N'gigafix_users', N'product', N'products')
GROUP BY t.name
ORDER BY t.name;

IF EXISTS (
    SELECT 1
    FROM sys.tables t
    JOIN sys.schemas s ON s.schema_id = t.schema_id
    JOIN sys.partitions p ON p.object_id = t.object_id AND p.index_id IN (0, 1)
    WHERE s.name = N'dbo'
      AND t.name IN (N'carts', N'cart_items', N'orders', N'order_items')
    GROUP BY t.object_id
    HAVING SUM(p.rows) > 0
)
    THROW 51001, '核心舊表已有資料；停止使用以空表為前提的 migration。', 1;

SELECT TABLE_NAME, COLUMN_NAME, DATA_TYPE, CHARACTER_MAXIMUM_LENGTH,
       NUMERIC_PRECISION, NUMERIC_SCALE, IS_NULLABLE
FROM INFORMATION_SCHEMA.COLUMNS
WHERE TABLE_SCHEMA = N'dbo'
  AND TABLE_NAME IN (N'carts', N'cart_items', N'orders', N'order_items',
                     N'cart', N'cart_item', N'order_item', N'payment', N'shipment')
ORDER BY TABLE_NAME, ORDINAL_POSITION;

SELECT t.name AS table_name, kc.name AS constraint_name, kc.type_desc
FROM sys.key_constraints kc
JOIN sys.tables t ON t.object_id = kc.parent_object_id
JOIN sys.schemas s ON s.schema_id = t.schema_id
WHERE s.name = N'dbo'
  AND t.name IN (N'carts', N'cart_items', N'orders', N'order_items')
ORDER BY t.name, kc.name;

SELECT OBJECT_NAME(fk.parent_object_id) AS table_name, fk.name,
       fk.is_disabled, fk.is_not_trusted,
       OBJECT_NAME(fk.referenced_object_id) AS referenced_table
FROM sys.foreign_keys fk
WHERE OBJECT_SCHEMA_NAME(fk.parent_object_id) = N'dbo'
  AND OBJECT_NAME(fk.parent_object_id) IN
      (N'carts', N'cart_items', N'orders', N'order_items')
ORDER BY table_name, fk.name;

/* 外部模組 FK 前置狀態；目前不得以不存在或未核准的目標建 FK。 */
SELECT OBJECT_ID(N'dbo.members', N'U') AS members_id,
       OBJECT_ID(N'dbo.gigafix_users', N'U') AS gigafix_users_id,
       OBJECT_ID(N'dbo.product', N'U') AS product_id,
       OBJECT_ID(N'dbo.products', N'U') AS products_id;
