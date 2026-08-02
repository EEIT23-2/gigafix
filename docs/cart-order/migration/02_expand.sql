/* Phase 1：表名切換、欄位擴充、新表與模組內 FK。尚未 DROP 舊欄位。 */
SET XACT_ABORT ON;

BEGIN TRY
    BEGIN TRANSACTION;

    IF OBJECT_ID(N'dbo.carts', N'U') IS NOT NULL AND OBJECT_ID(N'dbo.cart', N'U') IS NULL
        EXEC sys.sp_rename N'dbo.carts', N'cart';
    IF OBJECT_ID(N'dbo.cart_items', N'U') IS NOT NULL AND OBJECT_ID(N'dbo.cart_item', N'U') IS NULL
        EXEC sys.sp_rename N'dbo.cart_items', N'cart_item';
    IF OBJECT_ID(N'dbo.order_items', N'U') IS NOT NULL AND OBJECT_ID(N'dbo.order_item', N'U') IS NULL
        EXEC sys.sp_rename N'dbo.order_items', N'order_item';

    IF OBJECT_ID(N'dbo.cart', N'U') IS NULL OR OBJECT_ID(N'dbo.cart_item', N'U') IS NULL
       OR OBJECT_ID(N'dbo.orders', N'U') IS NULL OR OBJECT_ID(N'dbo.order_item', N'U') IS NULL
        THROW 51010, '核心表 rename 後不完整。', 1;

    IF COL_LENGTH(N'dbo.cart', N'user_id') IS NOT NULL AND COL_LENGTH(N'dbo.cart', N'member_id') IS NULL
        EXEC sys.sp_rename N'dbo.cart.user_id', N'member_id', N'COLUMN';
    IF COL_LENGTH(N'dbo.orders', N'user_id') IS NOT NULL AND COL_LENGTH(N'dbo.orders', N'member_id') IS NULL
        EXEC sys.sp_rename N'dbo.orders.user_id', N'member_id', N'COLUMN';

    IF COL_LENGTH(N'dbo.cart', N'version') IS NULL
        EXEC sys.sp_executesql N'
            ALTER TABLE dbo.cart ADD version BIGINT NULL;';
    IF EXISTS (
        SELECT 1 FROM sys.columns c JOIN sys.types ty ON ty.user_type_id = c.user_type_id
        WHERE c.object_id = OBJECT_ID(N'dbo.cart') AND c.name = N'version'
          AND ty.name <> N'bigint'
    )
        THROW 51011, 'cart.version 已存在但不是 BIGINT。', 1;
    EXEC sys.sp_executesql N'
        UPDATE dbo.cart SET version = 0 WHERE version IS NULL;';
    IF EXISTS (
        SELECT 1 FROM sys.columns
        WHERE object_id = OBJECT_ID(N'dbo.cart') AND name = N'version' AND is_nullable = 1
    )
        EXEC sys.sp_executesql N'
            ALTER TABLE dbo.cart ALTER COLUMN version BIGINT NOT NULL;';

    IF COL_LENGTH(N'dbo.orders', N'order_type') IS NULL
        EXEC sys.sp_executesql N'
            ALTER TABLE dbo.orders ADD order_type VARCHAR(20) NULL;';
    IF EXISTS (
        SELECT 1 FROM sys.columns c JOIN sys.types ty ON ty.user_type_id = c.user_type_id
        WHERE c.object_id = OBJECT_ID(N'dbo.orders') AND c.name = N'order_type'
          AND (ty.name <> N'varchar' OR c.max_length <> 20)
    )
        THROW 51012, 'orders.order_type 已存在但不是 VARCHAR(20)。', 1;

    EXEC sys.sp_executesql N'
        UPDATE dbo.orders SET order_type = ''GENERAL'' WHERE order_type IS NULL;';
    EXEC sys.sp_executesql N'
        IF EXISTS (
            SELECT 1 FROM dbo.orders
            WHERE order_type NOT IN (''GENERAL'', ''REPAIR'') OR order_type IS NULL
        )
            THROW 51013, ''orders.order_type 存在非法值。'', 1;';
    IF EXISTS (
        SELECT 1 FROM sys.columns
        WHERE object_id = OBJECT_ID(N'dbo.orders') AND name = N'order_type' AND is_nullable = 1
    )
        EXEC sys.sp_executesql N'
            ALTER TABLE dbo.orders ALTER COLUMN order_type VARCHAR(20) NOT NULL;';
    IF NOT EXISTS (SELECT 1 FROM sys.check_constraints WHERE parent_object_id = OBJECT_ID(N'dbo.orders') AND name = N'ck_orders_order_type')
        EXEC sys.sp_executesql N'
            ALTER TABLE dbo.orders WITH CHECK ADD CONSTRAINT ck_orders_order_type
                CHECK (order_type IN (''GENERAL'', ''REPAIR''));';

    ALTER TABLE dbo.orders ALTER COLUMN remark VARCHAR(500) NULL;
    EXEC sys.sp_executesql N'
        ALTER TABLE dbo.order_item ALTER COLUMN product_name VARCHAR(256) NOT NULL;';
    EXEC sys.sp_executesql N'
        ALTER TABLE dbo.order_item ALTER COLUMN unit_price DECIMAL(12,2) NOT NULL;';

    IF (
        SELECT COUNT(*)
        FROM sys.indexes i
        WHERE i.object_id = OBJECT_ID(N'dbo.cart_item')
          AND i.is_unique = 1
          AND i.is_disabled = 0
          AND (SELECT COUNT(*) FROM sys.index_columns ic
               WHERE ic.object_id = i.object_id AND ic.index_id = i.index_id AND ic.key_ordinal > 0) = 2
          AND EXISTS (SELECT 1 FROM sys.index_columns ic JOIN sys.columns c ON c.object_id = ic.object_id AND c.column_id = ic.column_id
                      WHERE ic.object_id = i.object_id AND ic.index_id = i.index_id AND ic.key_ordinal = 1 AND c.name = N'cart_id')
          AND EXISTS (SELECT 1 FROM sys.index_columns ic JOIN sys.columns c ON c.object_id = ic.object_id AND c.column_id = ic.column_id
                      WHERE ic.object_id = i.object_id AND ic.index_id = i.index_id AND ic.key_ordinal = 2 AND c.name = N'product_id')
    ) <> 1
        THROW 51016, 'cart_item 必須恰好有一組 cart_id、product_id 唯一鍵。', 1;
    EXEC sys.sp_executesql N'
        IF EXISTS (SELECT 1 FROM dbo.cart_item GROUP BY cart_id, product_id HAVING COUNT_BIG(*) > 1)
            THROW 51017, ''cart_item 存在重複 cart_id、product_id。'', 1;';

    IF NOT EXISTS (SELECT 1 FROM sys.foreign_keys WHERE parent_object_id = OBJECT_ID(N'dbo.cart_item') AND referenced_object_id = OBJECT_ID(N'dbo.cart'))
        EXEC sys.sp_executesql N'
            ALTER TABLE dbo.cart_item WITH CHECK ADD CONSTRAINT fk_cart_item_cart
                FOREIGN KEY (cart_id) REFERENCES dbo.cart(cart_id);';
    IF NOT EXISTS (SELECT 1 FROM sys.foreign_keys WHERE parent_object_id = OBJECT_ID(N'dbo.order_item') AND referenced_object_id = OBJECT_ID(N'dbo.orders'))
        EXEC sys.sp_executesql N'
            ALTER TABLE dbo.order_item WITH CHECK ADD CONSTRAINT fk_order_item_order
                FOREIGN KEY (order_id) REFERENCES dbo.orders(order_id);';

    IF OBJECT_ID(N'dbo.payment', N'U') IS NULL
    BEGIN
        CREATE TABLE dbo.payment (
            payment_id BIGINT IDENTITY(1,1) NOT NULL,
            order_id BIGINT NOT NULL,
            payment_method VARCHAR(30) NOT NULL,
            payment_status VARCHAR(20) NOT NULL,
            transaction_id VARCHAR(100) NULL,
            amount DECIMAL(12,2) NOT NULL,
            paid_at DATETIME2 NULL,
            created_at DATETIME2 NOT NULL,
            updated_at DATETIME2 NOT NULL,
            CONSTRAINT pk_payment PRIMARY KEY (payment_id),
            CONSTRAINT uq_payment_order UNIQUE (order_id),
            CONSTRAINT fk_payment_order FOREIGN KEY (order_id) REFERENCES dbo.orders(order_id),
            CONSTRAINT ck_payment_method CHECK (payment_method IN ('CREDIT_CARD','BANK_TRANSFER','CASH_ON_DELIVERY')),
            CONSTRAINT ck_payment_status CHECK (payment_status IN ('PENDING','PAID','PAYMENT_FAILED','REFUNDED','CANCELLED'))
        );
    END
    ELSE
        THROW 51014, 'dbo.payment 已存在；請先比對 metadata，不覆寫。', 1;

    IF OBJECT_ID(N'dbo.shipment', N'U') IS NULL
    BEGIN
        CREATE TABLE dbo.shipment (
            shipment_id BIGINT IDENTITY(1,1) NOT NULL,
            order_id BIGINT NOT NULL,
            receiver_name VARCHAR(50) NOT NULL,
            receiver_phone VARCHAR(20) NOT NULL,
            receiver_address VARCHAR(255) NOT NULL,
            shipping_method VARCHAR(30) NOT NULL,
            tracking_number VARCHAR(100) NULL,
            shipping_status VARCHAR(20) NOT NULL,
            shipped_at DATETIME2 NULL,
            delivered_at DATETIME2 NULL,
            created_at DATETIME2 NOT NULL,
            updated_at DATETIME2 NOT NULL,
            CONSTRAINT pk_shipment PRIMARY KEY (shipment_id),
            CONSTRAINT uq_shipment_order UNIQUE (order_id),
            CONSTRAINT fk_shipment_order FOREIGN KEY (order_id) REFERENCES dbo.orders(order_id),
            CONSTRAINT ck_shipment_method CHECK (shipping_method IN ('HOME_DELIVERY','CONVENIENCE_STORE')),
            CONSTRAINT ck_shipment_status CHECK (shipping_status IN ('PREPARING','SHIPPED','DELIVERED','CANCELLED'))
        );
    END
    ELSE
        THROW 51015, 'dbo.shipment 已存在；請先比對 metadata，不覆寫。', 1;

    /* BLOCKED_BY_MEMBER_MODULE: cart.member_id、orders.member_id FK 暫不建立。 */
    /* BLOCKED_BY_PRODUCT_MODULE: cart_item.product_id、order_item.product_id FK 暫不建立。 */

    COMMIT TRANSACTION;
END TRY
BEGIN CATCH
    IF @@TRANCOUNT > 0 ROLLBACK TRANSACTION;
    THROW;
END CATCH;

SELECT OBJECT_ID(N'dbo.cart', N'U') AS cart_id,
       OBJECT_ID(N'dbo.cart_item', N'U') AS cart_item_id,
       OBJECT_ID(N'dbo.orders', N'U') AS orders_id,
       OBJECT_ID(N'dbo.order_item', N'U') AS order_item_id,
       OBJECT_ID(N'dbo.payment', N'U') AS payment_id,
       OBJECT_ID(N'dbo.shipment', N'U') AS shipment_id;
