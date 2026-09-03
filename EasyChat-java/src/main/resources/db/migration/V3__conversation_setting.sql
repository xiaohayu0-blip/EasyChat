-- =====================================================
-- V3: 会话设置(置顶 / 免打扰)
-- 说明:
--  1. 系统里会话是"虚拟"的,由 message 表实时推导,没有独立的 conversation 表
--  2. 置顶/免打扰是"每个用户对每个会话"的偏好,因此需要一张独立表来存储
--  3. 不设 deleted:取消置顶/取消免打扰 = 把 pinned/muted 置回 0,不需要删行
--  4. uk_user_conv 唯一索引保证"一个用户对一个会话只有一行设置",天然支持 upsert
-- =====================================================

CREATE TABLE `conversation_setting`
(
    `id`                BIGINT   NOT NULL COMMENT '主键,雪花ID',
    `user_id`           BIGINT   NOT NULL COMMENT '设置所属用户ID',
    `conversation_type` TINYINT  NOT NULL DEFAULT 1 COMMENT '会话类型:1单聊 2群聊(预留)',
    `peer_id`           BIGINT   NOT NULL COMMENT '对方ID:单聊为好友ID,群聊为群ID',
    `pinned`            TINYINT  NOT NULL DEFAULT 0 COMMENT '是否置顶:0否 1是',
    `muted`             TINYINT  NOT NULL DEFAULT 0 COMMENT '是否免打扰:0否 1是',
    `create_time`       DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    `update_time`       DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_user_conv` (`user_id`, `conversation_type`, `peer_id`)
) ENGINE = InnoDB
  DEFAULT CHARSET = utf8mb4
  COLLATE = utf8mb4_general_ci COMMENT ='会话设置表';
