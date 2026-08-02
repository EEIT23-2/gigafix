/* Phase 1／2 後唯讀驗證。所有 issue_count 應為 0。 */
SET NOCOUNT ON;

WITH expected(table_name) AS (
    SELECT * FROM (VALUES (N'cart'),(N'cart_item'),(N'orders'),(N'order_item'),(N'payment'),(N'shipment')) v(table_name)
)
SELECT e.table_name, CASE WHEN t.object_id IS NULL THEN 1 ELSE 0 END AS issue_count
FROM expected e LEFT JOIN sys.tables t ON t.name=e.table_name AND SCHEMA_NAME(t.schema_id)=N'dbo';

SELECT name AS unexpected_legacy_table, 1 AS issue_count
FROM sys.tables WHERE SCHEMA_NAME(schema_id)=N'dbo' AND name IN (N'carts',N'cart_items',N'order_items');

WITH expected(table_name,column_name) AS (
    SELECT * FROM (VALUES
      (N'cart',N'cart_id'),(N'cart',N'member_id'),(N'cart',N'version'),(N'cart',N'status'),(N'cart',N'created_at'),(N'cart',N'updated_at'),
      (N'cart_item',N'cart_item_id'),(N'cart_item',N'cart_id'),(N'cart_item',N'product_id'),(N'cart_item',N'created_at'),(N'cart_item',N'updated_at'),
      (N'orders',N'order_id'),(N'orders',N'member_id'),(N'orders',N'order_type'),(N'orders',N'order_date'),(N'orders',N'total_amount'),(N'orders',N'status'),(N'orders',N'payment_status'),(N'orders',N'remark'),(N'orders',N'created_at'),(N'orders',N'updated_at'),
      (N'order_item',N'order_item_id'),(N'order_item',N'order_id'),(N'order_item',N'product_id'),(N'order_item',N'product_name'),(N'order_item',N'unit_price'),(N'order_item',N'created_at'),(N'order_item',N'updated_at'),
      (N'payment',N'payment_id'),(N'payment',N'order_id'),(N'payment',N'payment_method'),(N'payment',N'payment_status'),(N'payment',N'transaction_id'),(N'payment',N'amount'),(N'payment',N'paid_at'),(N'payment',N'created_at'),(N'payment',N'updated_at'),
      (N'shipment',N'shipment_id'),(N'shipment',N'order_id'),(N'shipment',N'receiver_name'),(N'shipment',N'receiver_phone'),(N'shipment',N'receiver_address'),(N'shipment',N'shipping_method'),(N'shipment',N'tracking_number'),(N'shipment',N'shipping_status'),(N'shipment',N'shipped_at'),(N'shipment',N'delivered_at'),(N'shipment',N'created_at'),(N'shipment',N'updated_at')) v(table_name,column_name)
), actual AS (
    SELECT TABLE_NAME table_name,COLUMN_NAME column_name FROM INFORMATION_SCHEMA.COLUMNS WHERE TABLE_SCHEMA=N'dbo'
)
SELECT N'MISSING' issue,e.table_name,e.column_name FROM expected e LEFT JOIN actual a ON a.table_name=e.table_name AND a.column_name=e.column_name WHERE a.column_name IS NULL
UNION ALL
SELECT N'EXTRA',a.table_name,a.column_name FROM actual a LEFT JOIN expected e ON e.table_name=a.table_name AND e.column_name=a.column_name WHERE a.table_name IN (N'cart',N'cart_item',N'orders',N'order_item',N'payment',N'shipment') AND e.column_name IS NULL;

SELECT N'cart.version null' check_name, COUNT_BIG(*) issue_count FROM dbo.cart WHERE version IS NULL
UNION ALL SELECT N'orders.order_type null',COUNT_BIG(*) FROM dbo.orders WHERE order_type IS NULL
UNION ALL SELECT N'orders.order_type enum',COUNT_BIG(*) FROM dbo.orders WHERE order_type NOT IN ('GENERAL','REPAIR')
UNION ALL SELECT N'payment required null',COUNT_BIG(*) FROM dbo.payment WHERE order_id IS NULL OR payment_method IS NULL OR payment_status IS NULL OR amount IS NULL OR created_at IS NULL OR updated_at IS NULL
UNION ALL SELECT N'payment method enum',COUNT_BIG(*) FROM dbo.payment WHERE payment_method NOT IN ('CREDIT_CARD','BANK_TRANSFER','CASH_ON_DELIVERY')
UNION ALL SELECT N'payment status enum',COUNT_BIG(*) FROM dbo.payment WHERE payment_status NOT IN ('PENDING','PAID','PAYMENT_FAILED','REFUNDED','CANCELLED')
UNION ALL SELECT N'shipment required null',COUNT_BIG(*) FROM dbo.shipment WHERE order_id IS NULL OR receiver_name IS NULL OR receiver_phone IS NULL OR receiver_address IS NULL OR shipping_method IS NULL OR shipping_status IS NULL OR created_at IS NULL OR updated_at IS NULL
UNION ALL SELECT N'shipment method enum',COUNT_BIG(*) FROM dbo.shipment WHERE shipping_method NOT IN ('HOME_DELIVERY','CONVENIENCE_STORE')
UNION ALL SELECT N'shipment status enum',COUNT_BIG(*) FROM dbo.shipment WHERE shipping_status NOT IN ('PREPARING','SHIPPED','DELIVERED','CANCELLED');

SELECT N'cart_item orphan' check_name,COUNT_BIG(*) issue_count FROM dbo.cart_item ci LEFT JOIN dbo.cart c ON c.cart_id=ci.cart_id WHERE c.cart_id IS NULL
UNION ALL SELECT N'order_item orphan',COUNT_BIG(*) FROM dbo.order_item oi LEFT JOIN dbo.orders o ON o.order_id=oi.order_id WHERE o.order_id IS NULL
UNION ALL SELECT N'payment orphan',COUNT_BIG(*) FROM dbo.payment p LEFT JOIN dbo.orders o ON o.order_id=p.order_id WHERE o.order_id IS NULL
UNION ALL SELECT N'shipment orphan',COUNT_BIG(*) FROM dbo.shipment s LEFT JOIN dbo.orders o ON o.order_id=s.order_id WHERE o.order_id IS NULL;

SELECT N'cart product duplicate' check_name,COUNT_BIG(*) issue_count FROM (SELECT cart_id,product_id FROM dbo.cart_item GROUP BY cart_id,product_id HAVING COUNT_BIG(*)>1) d
UNION ALL SELECT N'payment order duplicate',COUNT_BIG(*) FROM (SELECT order_id FROM dbo.payment GROUP BY order_id HAVING COUNT_BIG(*)>1) d
UNION ALL SELECT N'shipment order duplicate',COUNT_BIG(*) FROM (SELECT order_id FROM dbo.shipment GROUP BY order_id HAVING COUNT_BIG(*)>1) d;

SELECT fk.name,fk.is_disabled,fk.is_not_trusted
FROM sys.foreign_keys fk
WHERE fk.name IN (N'fk_cart_item_cart',N'fk_order_item_order',N'fk_payment_order',N'fk_shipment_order');
SELECT cc.name,cc.is_disabled,cc.is_not_trusted
FROM sys.check_constraints cc
WHERE cc.name IN (N'ck_orders_order_type',N'ck_payment_method',N'ck_payment_status',N'ck_shipment_method',N'ck_shipment_status');
SELECT N'cart_item cart_id product_id unique' check_name,
       CASE WHEN COUNT(*) = 1 THEN 0 ELSE 1 END issue_count
FROM sys.indexes i
WHERE i.object_id = OBJECT_ID(N'dbo.cart_item')
  AND i.is_unique = 1
  AND i.is_disabled = 0
  AND (SELECT COUNT(*) FROM sys.index_columns ic
       WHERE ic.object_id = i.object_id AND ic.index_id = i.index_id AND ic.key_ordinal > 0) = 2
  AND EXISTS (SELECT 1 FROM sys.index_columns ic JOIN sys.columns c ON c.object_id = ic.object_id AND c.column_id = ic.column_id
              WHERE ic.object_id = i.object_id AND ic.index_id = i.index_id AND ic.key_ordinal = 1 AND c.name = N'cart_id')
  AND EXISTS (SELECT 1 FROM sys.index_columns ic JOIN sys.columns c ON c.object_id = ic.object_id AND c.column_id = ic.column_id
              WHERE ic.object_id = i.object_id AND ic.index_id = i.index_id AND ic.key_ordinal = 2 AND c.name = N'product_id');

SELECT t.name table_name,i.name index_name,i.is_unique,i.is_primary_key
FROM sys.indexes i JOIN sys.tables t ON t.object_id=i.object_id
WHERE i.name IN (N'uq_payment_order',N'uq_shipment_order')
   OR (t.name IN (N'cart',N'cart_item',N'orders',N'order_item',N'payment',N'shipment') AND i.is_primary_key=1);
