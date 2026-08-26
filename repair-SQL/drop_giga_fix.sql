USE giga_fix;

DROP TABLE dbo.repairs;
DROP TABLE dbo.repair_technicians;
DROP TABLE dbo.stores;

select * from dbo.repairs;
select * from dbo.repair_technicians;
select * from dbo.stores;

SELECT * FROM dbo.members;
SELECT member_id, real_name FROM members;

----------------刪除資料庫所有表格-----------------------------------
USE giga_fix;

-- 先移除所有外鍵條件約束，避免表跟表之間有外鍵關聯，刪表時互相卡住
DECLARE @sql NVARCHAR(MAX) = N'';
SELECT @sql += 'ALTER TABLE ' + QUOTENAME(SCHEMA_NAME(fk.schema_id)) + '.' + QUOTENAME(OBJECT_NAME(fk.parent_object_id))
    + ' DROP CONSTRAINT ' + QUOTENAME(fk.name) + ';' + CHAR(13)
FROM sys.foreign_keys fk;
EXEC sp_executesql @sql;

-- 再刪除所有的表
SET @sql = N'';
SELECT @sql += 'DROP TABLE ' + QUOTENAME(SCHEMA_NAME(schema_id)) + '.' + QUOTENAME(name) + ';' + CHAR(13)
FROM sys.tables;
EXEC sp_executesql @sql;
------------------------------------------------------------------------