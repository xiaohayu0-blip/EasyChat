-- V5: message 表增加单向删除标记(仅自己视角隐藏,不回改对方)
ALTER TABLE `message`
    ADD COLUMN `deleted_by_sender` TINYINT NOT NULL DEFAULT 0 COMMENT '发送者是否删除:0否 1是' AFTER `status`,
    ADD COLUMN `deleted_by_receiver` TINYINT NOT NULL DEFAULT 0 COMMENT'接收者是否删除:0否 1是' AFTER `deleted_by_sender`;