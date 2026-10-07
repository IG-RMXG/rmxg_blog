-- =====================================================================
-- 04_sys_dict_data.sql
--
-- 与仓库内 backend/system-service/src/main/resources/sql/sys_dict_data.sql
-- 内容一致（原文件针对 ruoyi-vue 库导出，此处补上库名以便初始化时执行）。
-- 保持与仓库 SQL 同步：修改表结构时请同时更新这两处。
-- =====================================================================

USE `system`;

SET NAMES utf8mb4;
SET FOREIGN_KEY_CHECKS = 0;

-- ----------------------------
-- Table structure for sys_dict_data
-- ----------------------------
DROP TABLE IF EXISTS `sys_dict_data`;
CREATE TABLE `sys_dict_data`  (
  `dict_code` bigint NOT NULL AUTO_INCREMENT COMMENT '字典编码',
  `dict_sort` int NULL DEFAULT 0 COMMENT '字典排序',
  `dict_label` varchar(100) CHARACTER SET utf8mb4 COLLATE utf8mb4_bin NULL DEFAULT '' COMMENT '字典标签',
  `dict_value` varchar(100) CHARACTER SET utf8mb4 COLLATE utf8mb4_bin NULL DEFAULT '' COMMENT '字典键值',
  `dict_type` varchar(100) CHARACTER SET utf8mb4 COLLATE utf8mb4_bin NULL DEFAULT '' COMMENT '字典类型',
  `css_class` varchar(100) CHARACTER SET utf8mb4 COLLATE utf8mb4_bin NULL DEFAULT NULL COMMENT '样式属性（其他样式扩展）',
  `list_class` varchar(100) CHARACTER SET utf8mb4 COLLATE utf8mb4_bin NULL DEFAULT NULL COMMENT '表格回显样式',
  `is_default` char(1) CHARACTER SET utf8mb4 COLLATE utf8mb4_bin NULL DEFAULT 'N' COMMENT '是否默认（Y是 N否）',
  `status` char(1) CHARACTER SET utf8mb4 COLLATE utf8mb4_bin NULL DEFAULT '0' COMMENT '状态（0正常 1停用）',
  `create_by` varchar(64) CHARACTER SET utf8mb4 COLLATE utf8mb4_bin NULL DEFAULT '' COMMENT '创建者',
  `create_time` datetime NULL DEFAULT NULL COMMENT '创建时间',
  `update_by` varchar(64) CHARACTER SET utf8mb4 COLLATE utf8mb4_bin NULL DEFAULT '' COMMENT '更新者',
  `update_time` datetime NULL DEFAULT NULL COMMENT '更新时间',
  `remark` varchar(500) CHARACTER SET utf8mb4 COLLATE utf8mb4_bin NULL DEFAULT NULL COMMENT '备注',
  PRIMARY KEY (`dict_code`) USING BTREE
) ENGINE = InnoDB AUTO_INCREMENT = 100 CHARACTER SET = utf8mb4 COLLATE = utf8mb4_bin COMMENT = '字典数据表' ROW_FORMAT = Dynamic;

SET FOREIGN_KEY_CHECKS = 1;

-- ----------------------------
-- Records of sys_dict_data
--
-- 仓库导出的 .sql 只含表结构（不含 INSERT），而两张表的
-- AUTO_INCREMENT 均为 100，说明基准库中原本带有 RuoYi 标准字典数据。
-- 这里补回 RuoYi-Vue 的标准字典数据，使字典功能开箱可用。
-- 不需要的话整段删除即可。
-- ----------------------------
INSERT INTO `sys_dict_data`
  (`dict_sort`, `dict_label`, `dict_value`, `dict_type`, `css_class`, `list_class`, `is_default`, `status`, `create_by`, `create_time`, `remark`)
VALUES
  (1, '男',    '0', 'sys_user_sex',        '',   '',       'Y', '0', 'admin', NOW(), '性别男'),
  (2, '女',    '1', 'sys_user_sex',        '',   '',       'N', '0', 'admin', NOW(), '性别女'),
  (3, '未知',  '2', 'sys_user_sex',        '',   '',       'N', '0', 'admin', NOW(), '性别未知'),
  (1, '显示',  '0', 'sys_show_hide',       '',   'primary',  'Y', '0', 'admin', NOW(), '显示菜单'),
  (2, '隐藏',  '1', 'sys_show_hide',       '',   'danger',   'N', '0', 'admin', NOW(), '隐藏菜单'),
  (1, '正常',  '0', 'sys_normal_disable',  '',   'primary',  'Y', '0', 'admin', NOW(), '正常状态'),
  (2, '停用',  '1', 'sys_normal_disable',  '',   'danger',   'N', '0', 'admin', NOW(), '停用状态'),
  (1, '是',    'Y', 'sys_yes_no',          '',   'primary',  'Y', '0', 'admin', NOW(), '系统默认是'),
  (2, '否',    'N', 'sys_yes_no',          '',   'danger',   'N', '0', 'admin', NOW(), '系统默认否'),
  (1, '通知',  '1', 'sys_notice_type',     '',   'warning',  'Y', '0', 'admin', NOW(), '通知'),
  (2, '公告',  '2', 'sys_notice_type',     '',   'success',  'N', '0', 'admin', NOW(), '公告');
