-- =====================================================
-- V2: 朋友圈模块(动态 / 点赞 / 评论)
-- 说明:
--  1. moment(动态)、moment_comment(评论) 带逻辑删除 deleted
--  2. moment_like(点赞) 不逻辑删除:取消点赞 = 物理删,高频轻量无需保留
--  3. images 存图片 URL 的 JSON 数组(最多 9 张,在 Java 层校验)
--  4. reply_user_id = 0 表示直接评论动态,非 0 表示回复某人
-- =====================================================

-- 1. 动态表
CREATE TABLE `moment`
(
    `id`          BIGINT        NOT NULL COMMENT '主键,雪花ID',
    `user_id`     BIGINT        NOT NULL COMMENT '发布者用户ID',
    `content`     TEXT          NOT NULL COMMENT '文字内容,只发图时为空串',
    `images`      VARCHAR(2000) NOT NULL DEFAULT '' COMMENT '图片URL的JSON数组',
    `deleted`     TINYINT       NOT NULL DEFAULT 0 COMMENT '逻辑删除:0未删 1已删',
    `create_time` DATETIME      NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '发布时间',
    `update_time` DATETIME      NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    PRIMARY KEY (`id`),
    KEY `idx_user_time` (`user_id`, `create_time`)
) ENGINE = InnoDB
  DEFAULT CHARSET = utf8mb4
  COLLATE = utf8mb4_general_ci COMMENT ='动态表';

-- 2. 点赞表(不逻辑删除,取消点赞=物理删)
CREATE TABLE `moment_like`
(
    `id`          BIGINT   NOT NULL COMMENT '主键,雪花ID',
    `moment_id`   BIGINT   NOT NULL COMMENT '动态ID',
    `user_id`     BIGINT   NOT NULL COMMENT '点赞用户ID',
    `create_time` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '点赞时间',
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_moment_user` (`moment_id`, `user_id`)
) ENGINE = InnoDB
  DEFAULT CHARSET = utf8mb4
  COLLATE = utf8mb4_general_ci COMMENT ='点赞表';

-- 3. 评论表
CREATE TABLE `moment_comment`
(
    `id`            BIGINT       NOT NULL COMMENT '主键,雪花ID',
    `moment_id`     BIGINT       NOT NULL COMMENT '动态ID',
    `user_id`       BIGINT       NOT NULL COMMENT '评论人用户ID',
    `reply_user_id` BIGINT       NOT NULL DEFAULT 0 COMMENT '被回复的用户ID,0表示直接评论动态',
    `content`       VARCHAR(500) NOT NULL COMMENT '评论内容',
    `deleted`       TINYINT      NOT NULL DEFAULT 0 COMMENT '逻辑删除:0未删 1已删',
    `create_time`   DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '评论时间',
    PRIMARY KEY (`id`),
    KEY `idx_moment` (`moment_id`)
) ENGINE = InnoDB
  DEFAULT CHARSET = utf8mb4
  COLLATE = utf8mb4_general_ci COMMENT ='评论表';
