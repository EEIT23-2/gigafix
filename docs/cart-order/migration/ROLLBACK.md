# 六張正式表 migration rollback 說明

## Phase 1

Phase 1 的 `sp_rename`、ALTER 與 CREATE 位於同一個 SQL Server transaction；發生錯誤會由 `XACT_ABORT` 與 CATCH rollback。仍須先在 staging 驗證目前 SQL Server 版本與權限允許 transactional `sp_rename`。

若 Phase 1 已 commit，但新 Java 尚未依賴新結構，可依相反順序人工回復：

1. 確認 `payment`、`shipment` 沒有資料與外部依賴後，移除兩表。
2. 移除本次新增的四條內部 FK 與五條 CHECK。
3. 確認應用未使用 `order_type` 後移除該欄。
4. `remark` 縮回 255 前確認 `LEN(remark) <= 255`。
5. `product_name` 縮回 100 前確認 `LEN(product_name) <= 100`；`unit_price` 縮回 DECIMAL(10,2) 前確認值域。
6. 將 `member_id` rename 回 `user_id`。
7. 將 `cart`、`cart_item`、`order_item` rename 回 `carts`、`cart_items`、`order_items`。

既有系統自動命名 PK 不會在 Phase 1 被重建，因此 rollback 不處理 PK clustered index。

## Phase 2

Phase 2 DROP 的欄位不可透過簡單 `ALTER TABLE ADD` 還原原資料：

- `cart_item.quantity`
- `order_item.quantity`
- `order_item.subtotal`
- `orders.receiver_name`
- `orders.receiver_phone`
- `orders.shipping_address`
- `orders.shipping_fee`
- `orders.discount_amount`

完整回復必須依靠執行前 backup、archive table 或 database restore。只重新加入空欄位不等於 rollback。

## 外部模組阻塞

- `BLOCKED_BY_MEMBER_MODULE`：不得建立 `cart.member_id`、`orders.member_id` FK，直到正式會員表與 migration 核准。
- `BLOCKED_BY_PRODUCT_MODULE`：不得建立 `cart_item.product_id`、`order_item.product_id` FK，直到正式商品表與契約核准。
