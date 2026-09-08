-- docker-compose 裡的 db-init 服務會在 SQL Server 容器起來後執行這個腳本
-- 只負責建立 database，本身可重複執行（database 已存在就跳過）
IF DB_ID(N'giga_fix') IS NULL
BEGIN
    CREATE DATABASE giga_fix;
END
