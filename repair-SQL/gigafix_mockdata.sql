USE giga_fix;

-- ========== 測試會員：5筆 ==========
DECLARE @m1 BIGINT, @m2 BIGINT, @m3 BIGINT, @m4 BIGINT, @m5 BIGINT;

INSERT INTO members (email, password, real_name, nick_name, phone, address, gender, member_created_time)
VALUES ('test.repair1@gigafix.test', 'jack5241', '江政諺', '詹江村', '0912345671', '桃園市桃園區測試路1號', 'MALE', GETDATE());
SET @m1 = SCOPE_IDENTITY();

INSERT INTO members (email, password, real_name, nick_name, phone, address, gender, member_created_time)
VALUES ('test.repair2@gigafix.test', 'test1234', '林奕婷', '文文', '0912345672', '台北市大安區測試路2號', 'FEMALE', GETDATE());
SET @m2 = SCOPE_IDENTITY();

INSERT INTO members (email, password, real_name, nick_name, phone, address, gender, member_created_time)
VALUES ('test.repair3@gigafix.test', 'test1234', '曾能yo', 'AI大師', '0912345673', '新竹市東區測試路3號', 'MALE', GETDATE());
SET @m3 = SCOPE_IDENTITY();

INSERT INTO members (email, password, real_name, nick_name, phone, address, gender, member_created_time)
VALUES ('test.repair4@gigafix.test', 'test1234', '蔡佑佑', '阿豬', '0912345674', '桃園市中壢區測試路4號', 'FEMALE', GETDATE());
SET @m4 = SCOPE_IDENTITY();

INSERT INTO members (email, password, real_name, nick_name, phone, address, gender, member_created_time)
VALUES ('test.repair5@gigafix.test', 'test1234', '李建輝', '不只18歲', '0912345675', '台北市信義區測試路5號', 'MALE', GETDATE());
SET @m5 = SCOPE_IDENTITY();
-- 這5個帳號的密碼沒有經過雜湊，只能拿來當repairs的member_id用，不能拿去登入測試

-- ========== 分店：桃園2間、台北1間、新竹1間 ==========
DECLARE @taoyuan1 TINYINT, @taoyuan2 TINYINT, @taipei TINYINT, @hsinchu TINYINT;

INSERT INTO stores (store_name, store_address, store_phone)
VALUES ('桃園分店一', '桃園市桃園區中正路100號', '03-1234567');
SET @taoyuan1 = SCOPE_IDENTITY();

INSERT INTO stores (store_name, store_address, store_phone)
VALUES ('桃園分店二', '桃園市中壢區中央路200號', '03-2345678');
SET @taoyuan2 = SCOPE_IDENTITY();

INSERT INTO stores (store_name, store_address, store_phone)
VALUES ('台北分店', '台北市大安區忠孝東路300號', '02-3456789');
SET @taipei = SCOPE_IDENTITY();

INSERT INTO stores (store_name, store_address, store_phone)
VALUES ('新竹分店', '新竹市東區光復路400號', '03-4567890');
SET @hsinchu = SCOPE_IDENTITY();

-- ========== 技師：每間分店2位 ==========
DECLARE @t1 INT, @t2 INT, @t3 INT, @t4 INT, @t5 INT, @t6 INT, @t7 INT, @t8 INT;

INSERT INTO repair_technicians (technician_name, technician_phone, store_id)
VALUES ('李小輝', '0911111111', @taoyuan1);
SET @t1 = SCOPE_IDENTITY();

INSERT INTO repair_technicians (technician_name, technician_phone, store_id)
VALUES ('李小耀', '0911111112', @taoyuan1);
SET @t2 = SCOPE_IDENTITY();

INSERT INTO repair_technicians (technician_name, technician_phone, store_id)
VALUES ('蕭大程', '0922222221', @taoyuan2);
SET @t3 = SCOPE_IDENTITY();

INSERT INTO repair_technicians (technician_name, technician_phone, store_id)
VALUES ('劉美恩', '0922222222', @taoyuan2);
SET @t4 = SCOPE_IDENTITY();

INSERT INTO repair_technicians (technician_name, technician_phone, store_id)
VALUES ('拳志龍', '0933333331', @taipei);
SET @t5 = SCOPE_IDENTITY();

INSERT INTO repair_technicians (technician_name, technician_phone, store_id)
VALUES ('李佳蓉', '0933333332', @taipei);
SET @t6 = SCOPE_IDENTITY();

INSERT INTO repair_technicians (technician_name, technician_phone, store_id)
VALUES ('蔡昇達', '0944444441', @hsinchu);
SET @t7 = SCOPE_IDENTITY();

INSERT INTO repair_technicians (technician_name, technician_phone, store_id)
VALUES ('周雅婷', '0944444442', @hsinchu);
SET @t8 = SCOPE_IDENTITY();

-- ========== 維修單：12張，涵蓋9種狀態，技師id刻意重複使用 ==========
-- repair_status代碼：0待估價 1已報價 2維修中 3報價後不維修 4維修完成 5尚未取件 6已結案 7已取消 8未送檢
-- dropoff_type代碼：0親臨門市 1寄送門市
-- approval_status代碼：0待確認 1同意 2拒絕
-- repair_pay代碼：0門市 1線上；repair_pay_status代碼：0未付款 1已付款；pickup_type代碼：0自取 1宅配
-- 技師重複用量：王小明(t1)x3、陳小華(t2)x2、林大偉(t3)x2、張志豪(t5)x2

-- 1｜PENDING_QUOTE：還沒認領
INSERT INTO repairs (member_id, technician_id, store_id, repair_brand, repair_model, issue_description,
    booking_date, time_slot, dropoff_type, repair_status, estimated_cost,
    repair_created_time, repair_updated_time)
VALUES (@m1, NULL, @taoyuan1, 'Apple', 'iPhone 13', '螢幕破裂',
    DATEADD(DAY, 1, CAST(GETDATE() AS DATE)), '09:00', 0, 0, 0,
    GETDATE(), GETDATE());

-- 2｜QUOTED：王小明(t1)第1張
INSERT INTO repairs (member_id, technician_id, store_id, repair_brand, repair_model, issue_description,
    booking_date, time_slot, dropoff_type, repair_status, serial_number, inspection_result, repair_items,
    estimated_cost, approval_status, repair_created_time, repair_updated_time)
VALUES (@m2, @t1, @taoyuan1, 'Apple', 'iPhone 12', '無法充電',
    DATEADD(DAY, 2, CAST(GETDATE() AS DATE)), '10:00', 0, 1, 'SN0001', '充電孔氧化', '更換充電孔',
    800, 0, GETDATE(), GETDATE());

-- 3｜IN_REPAIR：王小明(t1)第2張
INSERT INTO repairs (member_id, technician_id, store_id, repair_brand, repair_model, issue_description,
    booking_date, time_slot, dropoff_type, repair_status, serial_number, inspection_result, repair_items,
    estimated_cost, approval_status, repair_created_time, repair_updated_time)
VALUES (@m3, @t1, @taoyuan1, 'Apple', 'iPhone SE', '電池膨脹',
    DATEADD(DAY, 3, CAST(GETDATE() AS DATE)), '11:00', 0, 2, 'SN0002', '電池膨脹需更換', '更換電池',
    1200, 1, GETDATE(), GETDATE());

-- 4｜REPAIR_COMPLETED：王小明(t1)第3張
INSERT INTO repairs (member_id, technician_id, store_id, repair_brand, repair_model, issue_description,
    booking_date, time_slot, dropoff_type, repair_status, serial_number, inspection_result, repair_items,
    estimated_cost, approval_status, final_cost, repair_created_time, repair_updated_time)
VALUES (@m4, @t1, @taoyuan1, 'Apple', 'iPhone XR', '喇叭無聲',
    DATEADD(DAY, 4, CAST(GETDATE() AS DATE)), '12:00', 0, 4, 'SN0003', '喇叭故障', '更換喇叭',
    900, 1, 900, GETDATE(), GETDATE());

-- 5｜QUOTE_REJECTED：陳小華(t2)第1張
INSERT INTO repairs (member_id, technician_id, store_id, repair_brand, repair_model, issue_description,
    booking_date, time_slot, dropoff_type, repair_status, serial_number, inspection_result, repair_items,
    estimated_cost, approval_status, repair_created_time, repair_updated_time)
VALUES (@m5, @t2, @taoyuan1, 'Apple', 'iPhone 11', '相機模糊',
    DATEADD(DAY, 5, CAST(GETDATE() AS DATE)), '13:00', 0, 3, 'SN0004', '鏡頭模組故障', '更換鏡頭模組',
    2500, 2, GETDATE(), GETDATE());

-- 6｜AWAITING_PICKUP：陳小華(t2)第2張
INSERT INTO repairs (member_id, technician_id, store_id, repair_brand, repair_model, issue_description,
    booking_date, time_slot, dropoff_type, repair_status, serial_number, inspection_result, repair_items,
    estimated_cost, approval_status, final_cost, repair_created_time, repair_updated_time)
VALUES (@m1, @t2, @taoyuan1, 'Apple', 'iPhone 14 Pro', '螢幕觸控失靈',
    DATEADD(DAY, 6, CAST(GETDATE() AS DATE)), '14:00', 0, 5, 'SN0005', '螢幕排線鬆脫', '更換螢幕總成',
    3200, 1, 3200, GETDATE(), GETDATE());

-- 7｜CLOSED：林大偉(t3)第1張
INSERT INTO repairs (member_id, technician_id, store_id, repair_brand, repair_model, issue_description,
    booking_date, time_slot, dropoff_type, repair_status, serial_number, inspection_result, repair_items,
    estimated_cost, approval_status, final_cost, repair_pay, repair_pay_status, pickup_type,
    repair_created_time, repair_updated_time)
VALUES (@m2, @t3, @taoyuan2, 'Apple', 'iPhone 15', '相機無法對焦',
    DATEADD(DAY, 7, CAST(GETDATE() AS DATE)), '15:00', 0, 6, 'SN0006', '相機模組損壞', '更換相機模組',
    1800, 1, 1800, 0, 1, 0, GETDATE(), GETDATE());

-- 8｜IN_REPAIR：林大偉(t3)第2張
INSERT INTO repairs (member_id, technician_id, store_id, repair_brand, repair_model, issue_description,
    booking_date, time_slot, dropoff_type, repair_status, serial_number, inspection_result, repair_items,
    estimated_cost, approval_status, repair_created_time, repair_updated_time)
VALUES (@m3, @t3, @taoyuan2, 'Apple', 'iPhone 13 mini', '進水故障',
    DATEADD(DAY, 8, CAST(GETDATE() AS DATE)), '16:00', 0, 2, 'SN0007', '主機板進水腐蝕', '主機板清洗維修',
    2800, 1, GETDATE(), GETDATE());

-- 9｜CANCELLED：客戶取消預約，還沒認領
INSERT INTO repairs (member_id, technician_id, store_id, repair_brand, repair_model, issue_description,
    booking_date, time_slot, dropoff_type, repair_status, estimated_cost,
    repair_created_time, repair_updated_time)
VALUES (@m4, NULL, @hsinchu, 'Apple', 'iPhone 13 Pro', '按鍵故障',
    DATEADD(DAY, 9, CAST(GETDATE() AS DATE)), '09:00', 0, 7, 0,
    GETDATE(), GETDATE());

-- 10｜NOT_DROPPED_OFF：吳建宏(t7)，客戶寄送但沒收到件
INSERT INTO repairs (member_id, technician_id, store_id, repair_brand, repair_model, issue_description,
    booking_date, time_slot, dropoff_type, repair_status, estimated_cost,
    repair_created_time, repair_updated_time)
VALUES (@m5, @t7, @hsinchu, 'Apple', 'iPhone XS', '麥克風收音異常',
    DATEADD(DAY, 10, CAST(GETDATE() AS DATE)), '10:00', 1, 8, 0,
    GETDATE(), GETDATE());

-- 11｜QUOTED：張志豪(t5)第1張
INSERT INTO repairs (member_id, technician_id, store_id, repair_brand, repair_model, issue_description,
    booking_date, time_slot, dropoff_type, repair_status, serial_number, inspection_result, repair_items,
    estimated_cost, approval_status, repair_created_time, repair_updated_time)
VALUES (@m1, @t5, @taipei, 'Apple', 'iPhone 16', '無法開機',
    DATEADD(DAY, 11, CAST(GETDATE() AS DATE)), '11:00', 0, 1, 'SN0008', '主機板供電異常', '更換主機板',
    4500, 0, GETDATE(), GETDATE());

-- 12｜CLOSED：張志豪(t5)第2張
INSERT INTO repairs (member_id, technician_id, store_id, repair_brand, repair_model, issue_description,
    booking_date, time_slot, dropoff_type, repair_status, serial_number, inspection_result, repair_items,
    estimated_cost, approval_status, final_cost, repair_pay, repair_pay_status, pickup_type,
    repair_created_time, repair_updated_time)
VALUES (@m2, @t5, @taipei, 'Apple', 'iPhone SE 2', '螢幕破裂',
    DATEADD(DAY, 12, CAST(GETDATE() AS DATE)), '12:00', 0, 6, 'SN0009', '面板破裂需更換', '更換螢幕總成',
    2200, 1, 2200, 0, 1, 0, GETDATE(), GETDATE());

--新增分類1
INSERT INTO categories (name)
VALUES ('測試');

--新增會員2的文章 (文章1)
INSERT INTO articles (category_id, author_id, parent_article_id, title, content, view_count, like_count, comment_count, cover_image, status, is_pinned, article_created_time, article_updated_time)
VALUES (1, 2, NULL, 'Spring Boot 實戰教學', '這是一篇介紹 Spring Data JPA 的文章內容...', 0, 0, 1, '', 1, 0, '2026-08-26 10:00:00', '2026-08-26 10:00:00');

--新增會員2 文章1下的留言
INSERT INTO comments (article_id, author_id, content, like_count, status, comment_created_time)
VALUES (1, 2, '這篇文章寫得很詳細，感謝分享！', 0, 0, '2026-08-26 15:30:00');

--新增會員3 文章1下的留言
INSERT INTO comments (article_id, author_id, content, like_count, status, comment_created_time)
VALUES (1, 3, '亦程香雞排真的好好吃呦！！', 0, 0, '2026-08-26 16:50:00');

--新增會員4 文章1下的留言
INSERT INTO comments (article_id, author_id, content, like_count, status, comment_created_time)
VALUES (1, 4, '我前天被前男友渣.....大家可以幫我集氣嗎....嗚嗚嗚嗚....', 0, 0, '2026-08-27 09:22:00');

--新增會員5 文章1下的留言
INSERT INTO comments (article_id, author_id, content, like_count, status, comment_created_time)
VALUES (1, 5, '大家應該都知道Gary不可能只有18歲吧？', 0, 0, '2026-08-26 12:30:00');

select * from admin_accounts;
select * from comments;


