-- =====================================================================
-- 00_create_databases.sql
--
-- 创建本项目后端所需的两个数据库：
--   nacos_dev : Nacos 服务端的配置/注册持久化库（见 docker-compose 中
--               MYSQL_SERVICE_DB_NAME=nacos_dev）
--   system    : system-service 的业务库（见 system-service/application.yml
--               jdbc:mysql://localhost:3306/system）
--
-- 该脚本由 MySQL 官方镜像首次启动时自动执行（docker-entrypoint-initdb.d）。
-- 注意：仅在数据卷 mysql_data 为空的首次启动时执行。
--       如需重新初始化，见 docker/mysql/README.md 的「重新初始化」一节。
-- =====================================================================

SET NAMES utf8mb4;

CREATE DATABASE IF NOT EXISTS `nacos_dev`
  DEFAULT CHARACTER SET utf8mb4
  DEFAULT COLLATE utf8mb4_general_ci;

CREATE DATABASE IF NOT EXISTS `system`
  DEFAULT CHARACTER SET utf8mb4
  DEFAULT COLLATE utf8mb4_bin;

-- 为 root 开放从任意宿主（含容器网络内的 nacos 容器）连接
ALTER USER 'root'@'%' IDENTIFIED BY '123456';
GRANT ALL PRIVILEGES ON *.* TO 'root'@'%' WITH GRANT OPTION;
FLUSH PRIVILEGES;

SELECT
  SCHEMA_NAME   AS `database`,
  DEFAULT_CHARACTER_SET_NAME AS `charset`,
  DEFAULT_COLLATION_NAME     AS `collation`
FROM information_schema.SCHEMATA
WHERE SCHEMA_NAME IN ('nacos_dev', 'system');
