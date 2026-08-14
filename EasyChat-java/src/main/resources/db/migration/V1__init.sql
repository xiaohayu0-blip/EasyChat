-- =====================================================
-- V1: 初始化核心表
-- 说明:
--  1. 主键 id 均为雪花 ID(BIGINT),由 MyBatis-Plus ASSIGN_ID 生成
--  2. user / friend_request 带逻辑删除 deleted(0未删 1已删)
--  3. friend / message 不逻辑删除:好友删除=物理删两条,消息记录需永久保留
--  4. create_time 用 DEFAULT CURRENT_TIMESTAMP,update_time 用 ON UPDATE 自动更新
-- =====================================================

-- 1. 用户表
CREATE TABLE `user`
(
    `id`              BIGINT       NOT NULL COMMENT '主键,雪花ID',
    `phone`           VARCHAR(20)  NOT NULL COMMENT '手机号,登录账号',
    `password`        VARCHAR(100) NOT NULL COMMENT 'BCrypt加密后的密码',
    `nickname`        VARCHAR(50)  NOT NULL DEFAULT '' COMMENT '昵称',
    `avatar`          VARCHAR(255) NOT NULL DEFAULT '' COMMENT '头像URL',
    `gender`          TINYINT      NOT NULL DEFAULT 0 COMMENT '性别:0未知 1男 2女',
    `signature`       VARCHAR(200) NOT NULL DEFAULT '' COMMENT '个性签名',
    `status`          TINYINT      NOT NULL DEFAULT 0 COMMENT '账号状态:0正常 1禁用',
    `last_login_time` DATETIME              DEFAULT NULL COMMENT '最后登录时间',
    `deleted`         TINYINT      NOT NULL DEFAULT 0 COMMENT '逻辑删除:0未删 1已删',
    `create_time`     DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    `update_time`     DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_phone` (`phone`)
) ENGINE = InnoDB
  DEFAULT CHARSET = utf8mb4
  COLLATE = utf8mb4_general_ci COMMENT ='用户表';

-- 2. 好友申请表
CREATE TABLE `friend_request`
(
    `id`           BIGINT       NOT NULL COMMENT '主键,雪花ID',
    `from_user_id` BIGINT       NOT NULL COMMENT '发起申请的用户ID',
    `to_user_id`   BIGINT       NOT NULL COMMENT '被申请的用户ID',
    `message`      VARCHAR(100) NOT NULL DEFAULT '' COMMENT '验证消息',
    `status`       TINYINT      NOT NULL DEFAULT 0 COMMENT '状态:0待处理 1已同意 2已拒绝',
    `handle_time`  DATETIME              DEFAULT NULL COMMENT '处理时间',
    `deleted`      TINYINT      NOT NULL DEFAULT 0 COMMENT '逻辑删除:0未删 1已删',
    `create_time`  DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    `update_time`  DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    PRIMARY KEY (`id`),
    KEY `idx_to_status` (`to_user_id`, `status`),
    KEY `idx_from_to` (`from_user_id`, `to_user_id`)
) ENGINE = InnoDB
  DEFAULT CHARSET = utf8mb4
  COLLATE = utf8mb4_general_ci COMMENT ='好友申请表';

-- 3. 好友表(已建立的好友关系,双向各存一条)
CREATE TABLE `friend`
(
    `id`          BIGINT      NOT NULL COMMENT '主键,雪花ID',
    `user_id`     BIGINT      NOT NULL COMMENT '用户ID',
    `friend_id`   BIGINT      NOT NULL COMMENT '好友ID',
    `remark`      VARCHAR(50) NOT NULL DEFAULT '' COMMENT '备注名',
    `create_time` DATETIME    NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '建立时间',
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_user_friend` (`user_id`, `friend_id`),
    KEY `idx_friend_id` (`friend_id`)
) ENGINE = InnoDB
  DEFAULT CHARSET = utf8mb4
  COLLATE = utf8mb4_general_ci COMMENT ='好友表';

-- 4. 消息表(单聊 + 群聊通用,不逻辑删除)
CREATE TABLE `message`
(
    `id`                BIGINT   NOT NULL COMMENT '主键,雪花ID',
    `conversation_type` TINYINT  NOT NULL COMMENT '会话类型:1单聊 2群聊',
    `from_user_id`      BIGINT   NOT NULL COMMENT '发送者用户ID',
    `to_id`             BIGINT   NOT NULL COMMENT '接收方ID:单聊为对方userId,群聊为groupId',
    `content_type`      TINYINT  NOT NULL DEFAULT 1 COMMENT '消息类型:1文本 2图片 3语音 4视频 5文件',
    `content`           TEXT     NOT NULL COMMENT '消息内容(文本或媒体URL)',
    `status`            TINYINT  NOT NULL DEFAULT 0 COMMENT '消息状态:0已发送 1已送达 2已读',
    `create_time`       DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '发送时间',
    PRIMARY KEY (`id`),
    KEY `idx_to_time` (`to_id`, `create_time`),
    KEY `idx_from_time` (`from_user_id`, `create_time`)
) ENGINE = InnoDB
  DEFAULT CHARSET = utf8mb4
  COLLATE = utf8mb4_general_ci COMMENT ='消息表';
