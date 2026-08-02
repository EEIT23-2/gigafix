/* Phase 2：不可逆移除舊欄位。先部署新 Java、完成備份並重跑 01_precheck.sql。 */
SET XACT_ABORT ON;
DECLARE @backup_confirmed BIT = 0; -- 執行人完成可還原備份後，當次執行明確改成 1。

IF @backup_confirmed <> 1
    THROW 51020, '未確認備份；禁止執行 Contract phase。', 1;
IF OBJECT_ID(N'dbo.cart_item', N'U') IS NULL OR OBJECT_ID(N'dbo.order_item', N'U') IS NULL OR OBJECT_ID(N'dbo.orders', N'U') IS NULL
    THROW 51021, '正式表尚未完整建立。', 1;

DECLARE @issues BIGINT = 0;

IF COL_LENGTH(N'dbo.cart_item', N'quantity') IS NOT NULL
BEGIN
    EXEC sys.sp_executesql
        N'SELECT @count = COUNT_BIG(*) FROM dbo.cart_item WHERE quantity IS NULL OR quantity <> 1;',
        N'@count BIGINT OUTPUT', @count = @issues OUTPUT;
    SELECT N'cart_item_quantity_not_one' AS check_name, @issues AS issue_count;
    IF @issues > 0 THROW 51022, 'cart_item.quantity 並非全為 1。', 1;
END

IF COL_LENGTH(N'dbo.order_item', N'quantity') IS NOT NULL
BEGIN
    SET @issues = 0;
    EXEC sys.sp_executesql
        N'SELECT @count = COUNT_BIG(*) FROM dbo.order_item WHERE quantity IS NULL OR quantity <> 1;',
        N'@count BIGINT OUTPUT', @count = @issues OUTPUT;
    SELECT N'order_item_quantity_not_one' AS check_name, @issues AS issue_count;
    IF @issues > 0 THROW 51023, 'order_item.quantity 並非全為 1。', 1;
END

IF COL_LENGTH(N'dbo.order_item', N'subtotal') IS NOT NULL
BEGIN
    SET @issues = 0;
    EXEC sys.sp_executesql
        N'SELECT @count = COUNT_BIG(*) FROM dbo.order_item WHERE subtotal IS NULL OR subtotal <> unit_price;',
        N'@count BIGINT OUTPUT', @count = @issues OUTPUT;
    SELECT N'order_item_subtotal_mismatch' AS check_name, @issues AS issue_count;
    IF @issues > 0 THROW 51024, 'order_item.subtotal 不等於 unit_price。', 1;
END

SELECT COUNT_BIG(*) AS orders_requiring_receiver_archive
FROM dbo.orders;

IF EXISTS (SELECT 1 FROM dbo.orders)
    THROW 51025, 'orders 有歷史資料；收件與費用欄位尚未封存或搬移。', 1;

BEGIN TRY
    BEGIN TRANSACTION;
    IF COL_LENGTH(N'dbo.cart_item', N'quantity') IS NOT NULL ALTER TABLE dbo.cart_item DROP COLUMN quantity;
    IF COL_LENGTH(N'dbo.order_item', N'quantity') IS NOT NULL ALTER TABLE dbo.order_item DROP COLUMN quantity;
    IF COL_LENGTH(N'dbo.order_item', N'subtotal') IS NOT NULL ALTER TABLE dbo.order_item DROP COLUMN subtotal;
    IF COL_LENGTH(N'dbo.orders', N'receiver_name') IS NOT NULL ALTER TABLE dbo.orders DROP COLUMN receiver_name;
    IF COL_LENGTH(N'dbo.orders', N'receiver_phone') IS NOT NULL ALTER TABLE dbo.orders DROP COLUMN receiver_phone;
    IF COL_LENGTH(N'dbo.orders', N'shipping_address') IS NOT NULL ALTER TABLE dbo.orders DROP COLUMN shipping_address;
    IF COL_LENGTH(N'dbo.orders', N'shipping_fee') IS NOT NULL ALTER TABLE dbo.orders DROP COLUMN shipping_fee;
    IF COL_LENGTH(N'dbo.orders', N'discount_amount') IS NOT NULL ALTER TABLE dbo.orders DROP COLUMN discount_amount;
    COMMIT TRANSACTION;
END TRY
BEGIN CATCH
    IF @@TRANCOUNT > 0 ROLLBACK TRANSACTION;
    THROW;
END CATCH;
