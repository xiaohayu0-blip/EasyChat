package com.gym.easychatjava.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.gym.easychatjava.entity.Message;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;


import java.util.List;

public interface MessageMapper extends BaseMapper<Message> {
    @Select("""
            SELECT m.id, m.from_user_id, m.to_id, m.content_type, m.content, m.create_time
              FROM message m
              WHERE m.id IN (
                  SELECT MAX(m2.id)
                  FROM message m2
                  WHERE m2.conversation_type = 1
                    AND (m2.from_user_id = #{userId} OR m2.to_id = #{userId})
                  GROUP BY CASE
                      WHEN m2.from_user_id = #{userId} THEN m2.to_id
                      ELSE m2.from_user_id
                  END
              )
              ORDER BY m.create_time DESC
            """
    )
    List<Message> listLatestMessages(@Param("userId") Long userId);
}
