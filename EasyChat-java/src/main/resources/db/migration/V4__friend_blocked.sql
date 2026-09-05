-- V4: friend 表增加 blocked 字段(好友拉黑)
-- 拉黑是单向状态,存"user_id 视角下是否拉黑了 friend_id"
-- 0 正常,1 已拉黑
ALTER TABLE `friend`
    ADD COLUMN `blocked` TINYINT NOT NULL DEFAULT 0 COMMENT '是否拉黑:0否 1是' AFTER `remark`;