package com.gym.easychatjava.common;

import lombok.Data;

/**
 * 通用响应结果类
 * @param <T>
 */
@Data
public class Result<T> {

    private int code;
    private T data;
    private String msg;

    //私有构造器,外部通过静态工厂方法创建
    private Result(){}

    /**
     * 成功无数据
     * @return
     * @param <T>
     */
    public static <T>Result<T> success(){
        Result<T> result = new Result<>();
        result.code = 0;
        result.data = null;
        result.msg = "success";
        return result;
    }

    /**
     * 成功带数据
     * @param data
     * @return
     * @param <T>
     */
    public static <T>Result<T> success(T data){
        Result<T> result = new Result<>();
        result.code = 0;
        result.data = data;
        result.msg = "success";
        return result;
    }

    /**
     * 自定义错误
     * @param code
     * @param msg
     * @return
     * @param <T>
     */
    public static <T>Result<T> fail(int code, String msg){
        Result<T> result = new Result<>();
        result.code = code;
        result.data = null;
        result.msg = msg;
        return result;
    }

    /**
     * 使用枚举中的code和msg
     * @param code
     * @return
     * @param <T>
     */
    public static <T>Result<T> fail(ResultCode code){
        Result<T> result = new Result<>();
        result.code = code.getCode();
        result.data = null;
        result.msg = code.getMsg();
        return result;
    }

}