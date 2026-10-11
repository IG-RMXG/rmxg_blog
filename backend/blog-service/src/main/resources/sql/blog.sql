-- =====================================================================
-- blog.sql —— 博客微服务（blog-service）建表 + 菜单/角色数据
--
-- 目标库：ry-cloud（与 system-service 共用同一个库）
--   之所以共用：菜单/角色/用户数据由 system-service 维护（sys_menu /
--   sys_role / sys_user / sys_user_role），blog-service 只读这些表做鉴权。
--   本脚本只负责 blog_* 五张自己的表和博客菜单。
--
-- 迁移到别的环境时：
--   - 若目标库没有 sys_menu / sys_role_menu，删掉下面的「菜单数据」段；
--   - 若目标库没有 sys_user / sys_role / sys_user_role，
--     需替换 cn.CDPersonal.blog.security 包内的鉴权实现（详见模块 README）。
--
-- 说明：
--   1) 建表（blog_category / blog_tag / blog_article / blog_article_tag / blog_comment）
--   2) 向 sys_menu 插入「博客管理」菜单（menu_id 2000 起）与按钮权限（F 类型）
--   3) 向 sys_role_menu 给「普通角色」role_id=2(common) 授权
--   菜单段幂等：先清理 2000~2099 区间的旧数据再插入。
-- =====================================================================

USE `ry-cloud`;

SET NAMES utf8mb4;

-- ---------------------------------------------------------------------
-- 表结构
-- ---------------------------------------------------------------------
DROP TABLE IF EXISTS `blog_article_tag`;
DROP TABLE IF EXISTS `blog_comment`;
DROP TABLE IF EXISTS `blog_article`;
DROP TABLE IF EXISTS `blog_tag`;
DROP TABLE IF EXISTS `blog_category`;

CREATE TABLE `blog_category` (
  `category_id`   bigint       NOT NULL AUTO_INCREMENT COMMENT '分类ID',
  `category_name` varchar(50)  NOT NULL                COMMENT '分类名称',
  `sort`          int          DEFAULT '0'             COMMENT '显示顺序',
  `status`        char(1)      DEFAULT '0'             COMMENT '状态（0正常 1停用）',
  `create_by`     varchar(64)  DEFAULT ''              COMMENT '创建者',
  `create_time`   datetime     DEFAULT NULL            COMMENT '创建时间',
  `update_by`     varchar(64)  DEFAULT ''              COMMENT '更新者',
  `update_time`   datetime     DEFAULT NULL            COMMENT '更新时间',
  `remark`        varchar(500) DEFAULT NULL            COMMENT '备注',
  PRIMARY KEY (`category_id`)
) ENGINE=InnoDB AUTO_INCREMENT=100 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_bin COMMENT='博客分类表';

CREATE TABLE `blog_tag` (
  `tag_id`      bigint       NOT NULL AUTO_INCREMENT COMMENT '标签ID',
  `tag_name`    varchar(50)  NOT NULL                COMMENT '标签名称',
  `sort`        int          DEFAULT '0'             COMMENT '显示顺序',
  `create_by`   varchar(64)  DEFAULT ''              COMMENT '创建者',
  `create_time` datetime     DEFAULT NULL            COMMENT '创建时间',
  `update_by`   varchar(64)  DEFAULT ''              COMMENT '更新者',
  `update_time` datetime     DEFAULT NULL            COMMENT '更新时间',
  `remark`      varchar(500) DEFAULT NULL            COMMENT '备注',
  PRIMARY KEY (`tag_id`)
) ENGINE=InnoDB AUTO_INCREMENT=100 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_bin COMMENT='博客标签表';

CREATE TABLE `blog_article` (
  `article_id`   bigint       NOT NULL AUTO_INCREMENT COMMENT '文章ID',
  `title`        varchar(200) NOT NULL                COMMENT '文章标题',
  `summary`      varchar(500) DEFAULT NULL            COMMENT '摘要',
  `content`      longtext                             COMMENT '正文（Markdown）',
  `cover_url`    varchar(500) DEFAULT NULL            COMMENT '封面地址',
  `category_id`  bigint       DEFAULT NULL            COMMENT '分类ID',
  `status`       char(1)      DEFAULT '0'             COMMENT '状态（0草稿 1已发布 2待审核 3已驳回）',
  `is_top`       char(1)      DEFAULT '0'             COMMENT '是否置顶（0否 1是）',
  `is_recommend` char(1)      DEFAULT '0'             COMMENT '是否推荐（0否 1是）',
  `view_count`   int          DEFAULT '0'             COMMENT '浏览量',
  `like_count`   int          DEFAULT '0'             COMMENT '点赞数',
  `comment_count` int         DEFAULT '0'             COMMENT '评论数',
  `user_id`      bigint       DEFAULT NULL            COMMENT '作者用户ID',
  `publish_time` datetime     DEFAULT NULL            COMMENT '发布时间',
  `create_by`    varchar(64)  DEFAULT ''              COMMENT '创建者',
  `create_time`  datetime     DEFAULT NULL            COMMENT '创建时间',
  `update_by`    varchar(64)  DEFAULT ''              COMMENT '更新者',
  `update_time`  datetime     DEFAULT NULL            COMMENT '更新时间',
  `remark`       varchar(500) DEFAULT NULL            COMMENT '备注',
  PRIMARY KEY (`article_id`),
  KEY `idx_category` (`category_id`),
  KEY `idx_user` (`user_id`),
  KEY `idx_status` (`status`)
) ENGINE=InnoDB AUTO_INCREMENT=1 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_bin COMMENT='博客文章表';

CREATE TABLE `blog_article_tag` (
  `article_id` bigint NOT NULL COMMENT '文章ID',
  `tag_id`     bigint NOT NULL COMMENT '标签ID',
  PRIMARY KEY (`article_id`, `tag_id`),
  KEY `idx_tag` (`tag_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_bin COMMENT='文章标签关联表';

CREATE TABLE `blog_comment` (
  `comment_id`  bigint        NOT NULL AUTO_INCREMENT COMMENT '评论ID',
  `article_id`  bigint        NOT NULL                COMMENT '文章ID',
  `parent_id`   bigint        DEFAULT '0'             COMMENT '父评论ID（0为顶级）',
  `content`     varchar(1000) NOT NULL                COMMENT '评论内容',
  `user_id`     bigint        DEFAULT NULL            COMMENT '评论者用户ID',
  `status`      char(1)       DEFAULT '0'             COMMENT '状态（0正常 1隐藏）',
  `create_by`   varchar(64)   DEFAULT ''              COMMENT '创建者',
  `create_time` datetime      DEFAULT NULL            COMMENT '创建时间',
  `update_by`   varchar(64)   DEFAULT ''              COMMENT '更新者',
  `update_time` datetime      DEFAULT NULL            COMMENT '更新时间',
  `remark`      varchar(500)  DEFAULT NULL            COMMENT '备注',
  PRIMARY KEY (`comment_id`),
  KEY `idx_article` (`article_id`)
) ENGINE=InnoDB AUTO_INCREMENT=1 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_bin COMMENT='博客评论表';

-- ---------------------------------------------------------------------
-- 菜单数据（menu_id 2000 起，幂等）
-- ---------------------------------------------------------------------
DELETE FROM `sys_role_menu` WHERE `menu_id` BETWEEN 2000 AND 2099;
DELETE FROM `sys_menu` WHERE `menu_id` BETWEEN 2000 AND 2099;

INSERT INTO `sys_menu`
  (`menu_id`, `menu_name`, `parent_id`, `order_num`, `path`, `component`, `query`, `route_name`, `is_frame`, `is_cache`, `menu_type`, `visible`, `status`, `perms`, `icon`, `create_by`, `create_time`, `remark`)
VALUES
  (2000, '博客管理', 0,    5, 'blog',     '',                    NULL, '', 1, 0, 'M', '0', '0', NULL,               'education',    'admin', NOW(), '博客模块'),
  (2001, '文章管理', 2000, 1, 'article',  'blog/article/index',  NULL, '', 1, 0, 'C', '0', '0', 'blog:article:list',  'documentation','admin', NOW(), NULL),
  (2002, '分类管理', 2000, 2, 'category', 'blog/category/index', NULL, '', 1, 0, 'C', '0', '0', 'blog:category:list', 'tree',         'admin', NOW(), NULL),
  (2003, '标签管理', 2000, 3, 'tag',      'blog/tag/index',      NULL, '', 1, 0, 'C', '0', '0', 'blog:tag:list',      'star',         'admin', NOW(), NULL),
  (2004, '评论管理', 2000, 4, 'comment',  'blog/comment/index',  NULL, '', 1, 0, 'C', '0', '0', 'blog:comment:list',  'message',      'admin', NOW(), NULL),
  (2010, '文章查询', 2001, 1, '', '', NULL, '', 1, 0, 'F', '0', '0', 'blog:article:query',   '#', 'admin', NOW(), NULL),
  (2011, '文章新增', 2001, 2, '', '', NULL, '', 1, 0, 'F', '0', '0', 'blog:article:add',     '#', 'admin', NOW(), NULL),
  (2012, '文章修改', 2001, 3, '', '', NULL, '', 1, 0, 'F', '0', '0', 'blog:article:edit',    '#', 'admin', NOW(), NULL),
  (2013, '文章删除', 2001, 4, '', '', NULL, '', 1, 0, 'F', '0', '0', 'blog:article:remove',  '#', 'admin', NOW(), NULL),
  (2014, '文章发布', 2001, 5, '', '', NULL, '', 1, 0, 'F', '0', '0', 'blog:article:publish', '#', 'admin', NOW(), NULL),
  (2015, '文章审核', 2001, 6, '', '', NULL, '', 1, 0, 'F', '0', '0', 'blog:article:audit',   '#', 'admin', NOW(), NULL),
  (2020, '分类查询', 2002, 1, '', '', NULL, '', 1, 0, 'F', '0', '0', 'blog:category:query',  '#', 'admin', NOW(), NULL),
  (2021, '分类新增', 2002, 2, '', '', NULL, '', 1, 0, 'F', '0', '0', 'blog:category:add',    '#', 'admin', NOW(), NULL),
  (2022, '分类修改', 2002, 3, '', '', NULL, '', 1, 0, 'F', '0', '0', 'blog:category:edit',   '#', 'admin', NOW(), NULL),
  (2023, '分类删除', 2002, 4, '', '', NULL, '', 1, 0, 'F', '0', '0', 'blog:category:remove', '#', 'admin', NOW(), NULL),
  (2030, '标签查询', 2003, 1, '', '', NULL, '', 1, 0, 'F', '0', '0', 'blog:tag:query',  '#', 'admin', NOW(), NULL),
  (2031, '标签新增', 2003, 2, '', '', NULL, '', 1, 0, 'F', '0', '0', 'blog:tag:add',    '#', 'admin', NOW(), NULL),
  (2032, '标签修改', 2003, 3, '', '', NULL, '', 1, 0, 'F', '0', '0', 'blog:tag:edit',   '#', 'admin', NOW(), NULL),
  (2033, '标签删除', 2003, 4, '', '', NULL, '', 1, 0, 'F', '0', '0', 'blog:tag:remove', '#', 'admin', NOW(), NULL),
  (2040, '评论查询', 2004, 1, '', '', NULL, '', 1, 0, 'F', '0', '0', 'blog:comment:query', '#', 'admin', NOW(), NULL),
  (2041, '评论删除', 2004, 2, '', '', NULL, '', 1, 0, 'F', '0', '0', 'blog:comment:remove','#', 'admin', NOW(), NULL),
  (2042, '评论审核', 2004, 3, '', '', NULL, '', 1, 0, 'F', '0', '0', 'blog:comment:audit', '#', 'admin', NOW(), NULL);

-- 普通角色(role_id=2, common)可访问：博客管理目录 + 文章管理 + 文章基础按钮
INSERT INTO `sys_role_menu` (`role_id`, `menu_id`) VALUES
  (2, 2000),
  (2, 2001),
  (2, 2010),
  (2, 2011),
  (2, 2012),
  (2, 2013);

-- ---------------------------------------------------------------------
-- 基础数据（便于开箱即用）
-- ---------------------------------------------------------------------
INSERT INTO `blog_category` (`category_id`, `category_name`, `sort`, `status`, `create_by`, `create_time`) VALUES
  (1, '技术笔记', 1, '0', 'admin', NOW()),
  (2, '生活随笔', 2, '0', 'admin', NOW());

INSERT INTO `blog_tag` (`tag_id`, `tag_name`, `sort`, `create_by`, `create_time`) VALUES
  (1, 'Java',   1, 'admin', NOW()),
  (2, 'Spring', 2, 'admin', NOW()),
  (3, '前端',   3, 'admin', NOW());

INSERT INTO `blog_article`
  (`article_id`, `title`, `summary`, `content`, `category_id`, `status`, `is_top`, `is_recommend`, `view_count`, `like_count`, `comment_count`, `user_id`, `publish_time`, `create_by`, `create_time`)
VALUES
  (1, '欢迎使用博客模块', '这是一篇示例文章，展示 Markdown 渲染效果。',
   '# 欢迎使用博客模块\n\n这是内置的示例文章，演示 **Markdown** 渲染效果。\n\n## 特性\n\n- 支持 **加粗**、*斜体*、`行内代码`\n- 支持列表、引用、代码块\n- 支持图片与链接\n\n## 代码示例\n\n```java\npublic class Hello {\n    public static void main(String[] args) {\n        System.out.println(\"Hello Blog\");\n    }\n}\n```\n\n> 管理员可在后台发布、审核文章；普通用户可撰写并提交自己的文章。',
   1, '1', '1', '1', 0, 0, 0, 1, NOW(), 'admin', NOW());

INSERT INTO `blog_article_tag` (`article_id`, `tag_id`) VALUES (1, 1), (1, 2);

INSERT INTO `blog_comment` (`article_id`, `parent_id`, `content`, `user_id`, `status`, `create_by`, `create_time`) VALUES
  (1, 0, '写得很清楚，收藏了！', 1, '0', 'admin', NOW());
