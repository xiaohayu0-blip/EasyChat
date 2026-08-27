package com.gym.easychatjava.vo;

import com.gym.easychatjava.entity.User;
import lombok.Data;
import org.springframework.beans.BeanUtils;

/**
 * 返回给前端的用户信息(不含密码等敏感字段)
 */
@Data
public class UserVO {

    /** 用户ID */
    private Long id;

    /** 手机号 */
    private String phone;

    /** 昵称 */
    private String nickname;

    /** 头像URL */
    private String avatar;

    /** 性别:0未知 1男 2女 */
    private Integer gender;

    /** 个性签名 */
    private String signature;

    /**
     * 把 User 实体转换成 UserVO(隐藏密码等敏感字段)
     * @param user 数据库查出来的用户实体
     * @return 不含敏感字段的视图对象
     */
    public static UserVO from(User user) {
        UserVO vo = new UserVO();
        BeanUtils.copyProperties(user, vo);
        return vo;
    }
}
